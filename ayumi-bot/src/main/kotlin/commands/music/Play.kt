package dev.deathblows.app.commands.music

import dev.arbjerg.lavalink.protocol.v4.LoadResult
import dev.arbjerg.lavalink.protocol.v4.Track
import dev.deathblows.app.core.ACCENT_COLOR
import dev.deathblows.app.core.commands.Command
import dev.deathblows.app.core.music.MusicManager
import dev.deathblows.app.core.music.SearchError
import dev.deathblows.app.core.utils.TtlCache
import dev.deathblows.app.core.utils.truncate
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.*
import dev.kord.common.entity.optional.Optional
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.suggest
import dev.kord.core.entity.interaction.ActionInteraction
import dev.kord.core.entity.interaction.GuildAutoCompleteInteraction
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.core.entity.interaction.GuildComponentInteraction
import dev.kord.rest.builder.component.interactionButtonAccessory
import dev.kord.rest.builder.component.section
import dev.kord.rest.builder.interaction.SubCommandBuilder
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.container
import dev.kord.rest.builder.message.messageFlags
import dev.schlaubi.lavakord.kord.getLink
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

private const val SEARCH_ADD_TO_QUEUE_CUSTOM_ID = "search_add_to_queue"

@Suppress("unused")
@OptIn(KordVoice::class)
class Play : Command(
	name = "play",
	description = "Plays a song from a given URL or search query",
	subcommandGroup = "music",
	commandTypesSupported = listOf(
		ApplicationCommandType.ChatInput,
	),
	componentCustomIds = listOf(SEARCH_ADD_TO_QUEUE_CUSTOM_ID),
	supportsAutocomplete = true
) {
	private val autocompleteCache = TtlCache<String, List<Choice>>()
	private val logger = LoggerFactory.getLogger(Play::class.java)
	private val debounces = mutableMapOf<Snowflake, Instant>()

	override fun registerChatInputCommand(builder: SubCommandBuilder) {
		return builder.string("query", "the search query or URL to play") {
			required = true
			autocomplete = true
		}
	}


	/**
	 * Currently this method spams the node too much,
	 * will refactor in the future
	 * @since 2026-03-15
	 */
	suspend fun onAutocompleteRunIGNORE(interaction: GuildAutoCompleteInteraction) {
		val query = interaction.focusedOption.value

		if (query.isEmpty()) {
			interaction.suggest(emptyList())
			return
		}

		val cached = autocompleteCache.get(query)
		if (cached != null) {
			interaction.suggest(cached)
			return
		}

		debounces[interaction.user.id]?.let {
			if (Clock.System.now() - it < 1.toDuration(DurationUnit.SECONDS)) {
				interaction.suggest(emptyList())
				return
			}
			debounces[interaction.user.id] = Clock.System.now()
		}

		val link = MusicManager.lavakord.getLink(interaction.guildId)
		val (results, error) = MusicManager.search(query, link, false)
		if (error != null) {
			logger.error("An error occurred when autocompleting play command: $error")
			interaction.suggest(emptyList())
			return
		}

		val tracks = when (results) {
			is LoadResult.SearchResult -> results.data.tracks
			is LoadResult.TrackLoaded -> listOf(results.data)
			is LoadResult.PlaylistLoaded -> results.data.tracks
			is LoadResult.NoMatches -> emptyList()
			is LoadResult.LoadFailed -> {
				logger.warn("An error occurred when loading tracks for autocomplete: $error")
				emptyList()
			}

			null -> emptyList()
		}

		interaction.suggest(tracks.map {
			Choice.StringChoice(
				name = it.info.title, value = it.info.uri.toString(),
				nameLocalizations = Optional.Missing()
			)
		})
	}

	override suspend fun onExternalInteractionRun(interaction: GuildComponentInteraction) {
		val uri = interaction.componentId.split("|").last()
		check(uri != "" && uri.startsWith("https://")) { "Play URL is not a valid URL: $uri" }

		val player = MusicManager.getPlayer(interaction) ?: run {
			logger.debug("No player found in component handler")
			return
		}
		val (uncheckedResult, error) = MusicManager.search(uri, player.link)

		error?.let {
			return respondError(error, interaction)
		}

		when (val result = uncheckedResult!!) {
			is LoadResult.TrackLoaded -> {
				player.enqueue(result.data, interaction.user)
				interaction.respondPublic {
					messageFlags {
						+MessageFlag.IsComponentsV2
					}
					container {
						accentColor = ACCENT_COLOR
						textDisplay("queued [${result.data.info.title}](${result.data.info.uri})!")
					}
				}
			}

			is LoadResult.NoMatches -> interaction.respondEphemeral { content = "no results!" }
			is LoadResult.LoadFailed -> interaction.respondEphemeral { content = "load failed!" }
			is LoadResult.PlaylistLoaded -> {
				val error =
					"unexpectedly got a playlist result when trying to load a track from the search results. this is likely a bug. playlist name: ${result.data.info.name}"
				interaction.respondPublic {
					content = error
				}
				logger.error(error)
			}

			else -> {
				val error =
					"unexpectedly got an unknown load result type when trying to load a track from the search results. this is likely a bug. result type: ${result::class}"
				interaction.respondPublic {
					content = error
				}
				logger.error(error)
			}
		}
	}

	override suspend fun onChatInputCommandRun(interaction: GuildChatInputCommandInteraction) {
		val query = interaction.command.strings["query"] ?: return

		val player = MusicManager.getPlayer(interaction) ?: run {
			logger.debug("No player found")
			return
		}

		val (uncheckedResult, error) = MusicManager.search(query, player.link)

		error?.let {
			return respondError(error, interaction)
		}

		when (val result = uncheckedResult!!) {
			is LoadResult.SearchResult -> {
				interaction.respondEphemeral {
					messageFlags {
						+MessageFlag.IsComponentsV2
					}
					container {
						accentColor = ACCENT_COLOR
						result.data.tracks.take(10).forEachIndexed { index, track ->
							val formattedMs =
								track.info.length.toDuration(DurationUnit.MILLISECONDS)
									.toComponents { _, minutes, seconds, _ ->
										"%d:%02d".format(minutes, seconds)
									}
							section {
								textDisplay("`${index + 1}.`[${track.info.title.truncate(60)}](${track.info.uri}) `${if (track.info.isStream) "`[LIVE]" else formattedMs}`")
								interactionButtonAccessory(
									ButtonStyle.Secondary,
									"$SEARCH_ADD_TO_QUEUE_CUSTOM_ID|${track.info.uri}"
								) {
									label = "＋ Queue"
								}
							}
						}
					}
				}
			}

			is LoadResult.PlaylistLoaded -> {
				val playlistName = result.data.info.name
				result.data.tracks.forEach { player.enqueue(it, interaction.user) }
				interaction.respondPublic {
					messageFlags {
						+MessageFlag.IsComponentsV2
					}
					container {
						accentColor = ACCENT_COLOR
						textDisplay("queued playlist `${playlistName}` with ${result.data.tracks.size} tracks!")
					}
				}
			}

			is LoadResult.TrackLoaded -> {
				player.enqueue(result.data, interaction.user)
				respondTrackLoaded(result.data, interaction)
			}

			is LoadResult.NoMatches -> interaction.respondEphemeral {
				messageFlags {
					+MessageFlag.IsComponentsV2
				}
				container {
					accentColor = ACCENT_COLOR
					textDisplay("no results found! try refining your search query?")
				}
			}

			is LoadResult.LoadFailed -> interaction.respondEphemeral {
				logger.warn("failed to load track for query \"$query\" in guild ${interaction.guildId}: ${result.data}")
				messageFlags {
					+MessageFlag.IsComponentsV2
				}
				container {
					accentColor = ACCENT_COLOR
					textDisplay("load failed! try refining your search results.")
				}
			}
		}

	}

	private suspend fun respondTrackLoaded(track: Track, interaction: ActionInteraction) {
		interaction.respondPublic {
			messageFlags {
				+MessageFlag.IsComponentsV2
			}
			container {
				accentColor = ACCENT_COLOR
				textDisplay("queued [${track.info.title}](${track.info.uri})!")
			}
		}
	}

	private suspend fun respondError(error: SearchError, interaction: ActionInteraction) {
		when (error) {
			SearchError.NotConnected -> interaction.respondEphemeral {
				content = "im not connected yet! wait a moment please!"
			}

			SearchError.NoMatches -> interaction.respondEphemeral { content = "no results!" }
			SearchError.LoadFailed -> interaction.respondEphemeral { content = "load failed!" }

		}

	}
}