package dev.deathblows.app

import dev.deathblows.app.core.commands.CommandManager
import dev.deathblows.app.core.music.MusicManager
import dev.kord.common.annotation.KordVoice
import dev.kord.core.Kord
import dev.kord.core.entity.interaction.GroupCommand
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.core.entity.interaction.RootCommand
import dev.kord.core.entity.interaction.SubCommand
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
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

		kord.on<ChatInputCommandInteractionCreateEvent> {
			when (interaction.command) {
				is RootCommand -> {
					val command = interaction.command as RootCommand
					val name = command.rootName

					println(CommandManager.nameAndRootToCommand)
					val foundCommand = CommandManager.nameAndRootToCommand[(name to "default")]
					foundCommand?.onChatInputCommandRun(interaction as GuildChatInputCommandInteraction) ?: run {
						logger.warn("No command found for name: $name and root: default")
					}
				}

				is SubCommand -> {
					val command = interaction.command as SubCommand

					val name = command.name
					val rootName = command.rootName

					val foundCommand = CommandManager.nameAndRootToCommand[(name to rootName)]
						?: CommandManager.nameAndRootToCommand[(name to "default")]

					foundCommand?.onChatInputCommandRun(interaction as GuildChatInputCommandInteraction) ?: run {
						logger.warn("No command found for name: $name and root: $rootName")
					}
				}

				is GroupCommand -> {
					logger.info("Received group command: ${interaction.command}")
				}
			}
		}
	}

}