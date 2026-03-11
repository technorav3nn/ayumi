package dev.deathblows.app.core.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.MessageFlag
import dev.kord.common.entity.SeparatorSpacingSize
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.VoiceState
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.rest.builder.component.actionRow
import dev.kord.rest.builder.message.container
import dev.kord.rest.builder.message.messageFlags
import dev.lavalink.youtube.YoutubeAudioSourceManager
import dev.lavalink.youtube.YoutubeSourceOptions
import dev.lavalink.youtube.clients.AndroidVrWithThumbnail
import kotlinx.coroutines.suspendCancellableCoroutine
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(KordVoice::class)
@KordVoice
object MusicManager {
	val logger: Logger = LoggerFactory.getLogger(MusicManager::class.java)
	val playerManager = DefaultAudioPlayerManager()
	val players: MutableMap<Snowflake, GuildPlayer> = mutableMapOf()

	private lateinit var kord: Kord

	fun initialize(kord: Kord) {
		val options = YoutubeSourceOptions()
			.setAllowSearch(true)
			.setRemoteCipher("https://cipher.kikkia.dev/", null, "AyumiBot/0.1.0 (tysm for hosting this!)");

		val youtubeAudioSourceManager = YoutubeAudioSourceManager(options, AndroidVrWithThumbnail())
		

		playerManager.registerSourceManager(youtubeAudioSourceManager)
		AudioSourceManagers.registerRemoteSources(playerManager);
		AudioSourceManagers.registerLocalSource(playerManager)

		logger.info("Music Manager initialized")
		this.kord = kord
	}

	suspend fun createPlayer(guildId: Snowflake, voiceState: VoiceState) = runCatching {
		val player = playerManager.createPlayer()

		val channel = voiceState.getChannelOrNull() ?: throw IllegalStateException("User is not in a voice channel")

		val guildPlayer = GuildPlayer(player)

		val trackScheduler = TrackScheduler(guildPlayer)
		player.addListener(trackScheduler)
		players[guildId] = guildPlayer

		logger.info("Created new GuildPlayer for guild ${guildId.value}")
		guildPlayer.connect(channel)

		return@runCatching guildPlayer
	}

	suspend fun getPlayerOrNull(interaction: GuildChatInputCommandInteraction): GuildPlayer? {
		val player = players[interaction.guildId]
		if (player != null) {
			return player
		}
		val voiceState = interaction.user.asMember().getVoiceStateOrNull()
		if (voiceState == null) {
			interaction.respondEphemeral {
				messageFlags {
					+MessageFlag.IsComponentsV2
				}
				container {
					textDisplay("**you need to be in a voice channel to use this command!**")
					separator(SeparatorSpacingSize.Small)
					actionRow {
						interactionButton(ButtonStyle.Secondary, "try-again") {
							label = "try again!"
						}
					}
				}
			}
			return null
		}
		return createPlayer(interaction.guildId, voiceState).getOrThrow()
	}

	suspend fun findTracks(query: String, search: Boolean = true): List<AudioTrack> =
		suspendCancellableCoroutine {
			this.playerManager.loadItem("${if (search) "ytsearch: " else ""}$query", object : AudioLoadResultHandler {

				override fun trackLoaded(track: AudioTrack) {
					logger.info(
						"Single track loaded for $query, ${track.info.uri}"
					)
					it.resume(listOf(track))
				}

				override fun playlistLoaded(playlist: AudioPlaylist) {
					logger.info("Playlist loaded for query $query, ${playlist.tracks.size} tracks")
					it.resume(playlist.tracks)
				}

				override fun noMatches() {
					logger.warn("No matches found for query $query")
					it.resumeWithException(RuntimeException("No tracks loaded"))
				}

				override fun loadFailed(exception: FriendlyException?) {
					logger.error("Failed to load track for query $query", exception)
					it.resumeWithException(exception ?: RuntimeException("Unknown exception"))
				}
			})
		}
}

suspend fun DefaultAudioPlayerManager.playTrack(query: String, player: AudioPlayer): AudioTrack {
	val track = suspendCancellableCoroutine<AudioTrack> {
		this.loadItem(query, object : AudioLoadResultHandler {
			override fun trackLoaded(track: AudioTrack) {
				it.resume(track)
			}

			override fun playlistLoaded(playlist: AudioPlaylist) {
				it.resume(playlist.tracks.first())
			}

			override fun noMatches() {
				TODO()
			}

			override fun loadFailed(exception: FriendlyException?) {
				TODO()
			}
		})
	}

	player.playTrack(track)

	return track
}