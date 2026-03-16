package dev.deathblows.app

import dev.deathblows.app.core.commands.CommandManager
import dev.deathblows.app.core.config.ConfigManager
import dev.deathblows.app.core.music.MusicManager
import dev.kord.common.annotation.KordVoice
import dev.kord.core.Kord
import dev.kord.core.entity.interaction.GroupCommand
import dev.kord.core.entity.interaction.RootCommand
import dev.kord.core.entity.interaction.SubCommand
import dev.kord.core.event.interaction.GuildAutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.GuildComponentInteractionCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import org.slf4j.LoggerFactory

const val TOKEN = "MTQ3OTE1OTQyNDcwOTgyNDU5Mw.G4YTOp.NWC6c1qiIDfaslGI9QKKsLGoVotj-_v9Z41t-o"

@KordVoice
@OptIn(KordVoice::class)
class Ayumi {
	private val logger = LoggerFactory.getLogger(Ayumi::class.java)
	private lateinit var kord: Kord

	suspend fun start() {
		runCatching { ConfigManager.initialize() }.onFailure { logger.error(it.message, it) }

		logger.info("Logging in...")
		val kord = Kord(TOKEN)
		this.kord = kord

		kord.login {
			intents = Intents(
				Intent.Guilds,
				Intent.GuildMessages,
				Intent.Guilds,
				Intent.GuildVoiceStates,
			)
			logger.info("Logged in as ${kord.getSelf().username}!")
			initializeListeners()
			runCatching { CommandManager.initialize(kord) }.onFailure { logger.error(it.message, it) }
			runCatching { MusicManager.initialize(kord) }.onFailure { logger.error(it.message, it) }
		}
	}

	private fun initializeListeners() {
		val logger = LoggerFactory.getLogger("dev.deathblows.app.Ayumi:Listeners")

		kord.on<GuildAutoCompleteInteractionCreateEvent> {
			logger.info("autcomplete create")
			when (interaction.command) {
				is RootCommand -> {
					val command = interaction.command as RootCommand
					val name = command.rootName

					val foundCommand = CommandManager.nameAndRootToCommand[(name to "default")]
					if (foundCommand?.supportsAutocomplete == null || !foundCommand.supportsAutocomplete) return@on
					foundCommand.onAutocompleteRun(interaction)
				}

				is SubCommand -> {
					val command = interaction.command as SubCommand

					val name = command.name
					val rootName = command.rootName

					val foundCommand = CommandManager.nameAndRootToCommand[(name to rootName)]
						?: CommandManager.nameAndRootToCommand[(name to "default")]

					if (foundCommand?.supportsAutocomplete == null || !foundCommand.supportsAutocomplete) return@on
					foundCommand.onAutocompleteRun(interaction)
				}

				is GroupCommand -> {
					logger.warn("Received autocomplete interaction for group command: ${interaction.command}, but is unhandled")
				}
			}
		}

		kord.on<GuildComponentInteractionCreateEvent> {
			val customId = interaction.componentId
			CommandManager.interactionHandlers.forEach { (ids, handler) ->
				if (ids.find { id -> customId.startsWith(id) } != null) {
					logger.debug("Found handler for custom ID: {}, executing...", customId)
					handler(interaction)
				}
			}
		}

		kord.on<GuildChatInputCommandInteractionCreateEvent> {
			when (interaction.command) {
				is RootCommand -> {
					val command = interaction.command as RootCommand
					val name = command.rootName

					val foundCommand = CommandManager.nameAndRootToCommand[(name to "default")]
					foundCommand?.onChatInputCommandRun(interaction) ?: run {
						logger.warn("No command found for name: $name and root: default")
					}
				}

				is SubCommand -> {
					val command = interaction.command as SubCommand

					val name = command.name
					val rootName = command.rootName

					val foundCommand = CommandManager.nameAndRootToCommand[(name to rootName)]
						?: CommandManager.nameAndRootToCommand[(name to "default")]

					foundCommand?.onChatInputCommandRun(interaction) ?: run {
						logger.warn("No command found for name: $name and root: $rootName")
					}
				}

				is GroupCommand -> {
					logger.warn("Received group command: ${interaction.command}, but is unhandled")
				}
			}
		}
	}

}