package dev.deathblows.app.core.music

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.arbjerg.lavalink.protocol.v4.LoadResult
import dev.deathblows.app.core.ERROR_COLOR
import dev.deathblows.app.core.WARNING_COLOR
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.MessageFlag
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.VoiceState
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.interaction.ActionInteraction
import dev.kord.core.entity.interaction.GuildInteraction
import dev.kord.rest.builder.message.container
import dev.kord.rest.builder.message.messageFlags
import dev.schlaubi.lavakord.LavaKord
import dev.schlaubi.lavakord.audio.Link
import dev.schlaubi.lavakord.kord.getLink
import dev.schlaubi.lavakord.kord.lavakord
import dev.schlaubi.lavakord.plugins.lavasrc.LavaSrc
import dev.schlaubi.lavakord.rest.loadItem
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

enum class CreatePlayerError {
	NotInVoiceChannel,
	FailedToConnect,
}

@OptIn(KordVoice::class)
@KordVoice
object MusicManager {
	val players: ConcurrentHashMap<Snowflake, GuildPlayer> = ConcurrentHashMap()
	private val playersMutex = Mutex()

	private val logger: Logger = LoggerFactory.getLogger(MusicManager::class.java)
	private lateinit var kord: Kord
	lateinit var lavakord: LavaKord

	fun initialize(kord: Kord) {
		this.kord = kord
		lavakord = kord.lavakord {
			plugins {
				install(LavaSrc)
			}
		}
		lavakord.addNode("ws://localhost:2333", "youshallnotpass")
		logger.info("Connected to LavaLink node at ws://localhost:2333")
	}

	fun hasPlayer(guildId: Snowflake): Boolean {
		return players.containsKey(guildId)
	}

	suspend fun <T> getPlayer(interaction: T): GuildPlayer?
			where T : ActionInteraction, T : GuildInteraction {
		players[interaction.guildId]?.let { return it }
		playersMutex.withLock {
			val voiceState = interaction.user.asMember().getVoiceStateOrNull()
			val (newPlayer, error) = createPlayer(interaction.guildId, voiceState, interaction.channel.asChannel())
			if (error != null) {
				when (error) {
					CreatePlayerError.NotInVoiceChannel -> {
						interaction.respondEphemeral {
							messageFlags {
								+MessageFlag.IsComponentsV2
							}
							container {
								accentColor = WARNING_COLOR
								textDisplay("You must be in a voice channel to use this command!")
							}
						}
					}

					CreatePlayerError.FailedToConnect -> {
						interaction.respondEphemeral {
							messageFlags {
								+MessageFlag.IsComponentsV2
							}
							container {
								accentColor = ERROR_COLOR
								textDisplay("Failed to connect to the voice channel! Please try again later.")
							}
						}
					}
				}
				return null
			}

			return newPlayer
		}
	}

	suspend fun createPlayer(
		guildId: Snowflake,
		voiceState: VoiceState?,
		textChannel: GuildMessageChannel
	): Result<GuildPlayer, CreatePlayerError> {
		val channelId = voiceState?.channelId
		val link = lavakord.getLink(guildId)

		if (channelId == null) {
			return Err(CreatePlayerError.NotInVoiceChannel)
		}

		link.connectAudio(channelId.value)
		val player = GuildPlayer(link, textChannel)
		players[guildId] = player
		return Ok(player)
	}

	suspend fun destroyPlayer(guildId: Snowflake) = playersMutex.withLock {
		players.remove(guildId)?.let { player ->
			player.link.disconnectAudio()
			player.link.destroy()
		}
	}

	suspend fun search(query: String, link: Link, mustBeConnected: Boolean = true): Result<LoadResult, SearchError> {
		val search = if (query.startsWith("http")) {
			query
		} else {
			"ytsearch:$query"
		}

		// okay to check CONNECTING, since it's not gonna stop track loading
		if (mustBeConnected && link.state != Link.State.CONNECTED && link.state != Link.State.CONNECTING) {
			return Err(SearchError.NotConnected)
		}

		return when (val item = link.loadItem(search)) {
			is LoadResult.TrackLoaded -> Ok(item)
			is LoadResult.PlaylistLoaded -> Ok(item)
			is LoadResult.SearchResult -> Ok(item)
			is LoadResult.NoMatches -> Err(SearchError.NoMatches)
			is LoadResult.LoadFailed -> Err(SearchError.LoadFailed)
		}
	}

}

enum class SearchError {
	NotConnected,
	NoMatches,
	LoadFailed,
}
