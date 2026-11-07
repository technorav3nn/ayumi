package dev.deathblows.app.core.commands

import dev.kord.common.entity.ApplicationCommandType
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.interaction.ComponentInteraction
import dev.kord.core.entity.interaction.GuildComponentInteraction
import dev.kord.rest.builder.interaction.*
import io.github.classgraph.ClassGraph
import kotlinx.coroutines.flow.launchIn
import org.slf4j.LoggerFactory

const val COMMAND_PACKAGE = "dev.deathblows.app.commands"
val DEV_GUILD_ID = Snowflake(956642632090677268)

object CommandManager {
	val commands = mutableListOf<Command>()
	val interactionHandlers = mutableListOf<Pair<List<String>, suspend (interaction: ComponentInteraction) -> Unit>>()

	// use a map for faster retrieval
	val nameAndRootToCommand = mutableMapOf<Pair<String, String?>, Command>()

	private val commandsToRegister =
		mutableListOf<(builder: MultiApplicationCommandBuilder) -> MultiApplicationCommandBuilder>()

	private val logger = LoggerFactory.getLogger(CommandManager::class.java)
	private val isProd = System.getenv("AYUMI_ENV") == "prod"
	private val shouldRegisterInDev = !isProd && System.getenv("REGISTER") != null

	suspend fun initialize(kord: Kord) {
		val scanned = ClassGraph().acceptPackages(COMMAND_PACKAGE).scan()
		val classes = scanned.allClasses
			.filter { it.extendsSuperclass(Command::class.java.name) }
			.groupBy s@{
				// get the subcommandGroup property from the class
				val loaded = it.loadClass().getDeclaredConstructor().newInstance() as Command
				return@s loaded.subcommandGroup ?: "default"
			}

		// added ( and ) to use Kotlin's forEach, not java.util.Map's
		classes.forEach { (group, infos) ->
			val cmds = infos.map { it.loadClass().getDeclaredConstructor().newInstance() as Command }

			cmds.forEach {
				val customIds = it.componentCustomIds
				if (customIds.isNotEmpty()) {
					logger.debug("Registering component custom IDs for command: {}, IDs: {}", it.name, customIds)
					interactionHandlers.add(
						Pair(customIds) { interaction ->
							logger.debug(
								"Handling interaction for command: {}, custom ID: {}",
								it.name,
								interaction.componentId
							)
							it.onExternalInteractionRun(interaction as GuildComponentInteraction)
						}
					)
				}
				addCommand(it)
			}

			prepareCommandsForGroup(cmds, group, kord)
			logger.info("Successfully loaded ${infos.size} commands for subcommand group: $group")
		}


		// setup the nameAndRootToCommand map for easy retrieval later
		commands.forEach { command ->
			val key = Pair(command.name, command.subcommandGroup)
			nameAndRootToCommand[key] = command
		}

		// check if we should even register them
		if (!isProd && !shouldRegisterInDev) {
			logger.info("Not registering commands because we're in dev and REGISTER env variable is not set.")
			return
		}

		// now we register the commands
		if (shouldRegisterInDev) {
			logger.info("Registering commands...")
			kord.createGuildApplicationCommands(DEV_GUILD_ID) {
				this@CommandManager.commandsToRegister
					.forEach { builder ->
						builder(this)
					}
			}.launchIn(kord)
		} else {
			logger.info("Registering global commands...")
			kord.createGlobalApplicationCommands {
				this@CommandManager.commandsToRegister
					.forEach { builder ->
						builder(this)
					}
			}.launchIn(kord)
		}

	}

	private fun addCommand(command: Command) {
		if (!commands.contains(command)) {
			commands.add(command)
		}
	}

	private fun prepareCommandsForGroup(commands: List<Command>, group: String?, kord: Kord) {
		logger.info("Preparing commands for group: {}", group ?: "default")

		val cmds = { builder: MultiApplicationCommandBuilder ->
			builder.apply {
				if (group == null || group == "default") {
					commands.forEach {
						if (it.commandTypesSupported.contains(ApplicationCommandType.ChatInput)) {
							this.input(it.name, it.description) {
								it.registerChatInputCommand(this as GlobalChatInputCreateBuilder)
							}
						}
						if (it.commandTypesSupported.contains(ApplicationCommandType.User)) {
							this.user(it.name) {
								it.registerUserContextCommand(this as GlobalUserCommandCreateBuilder)
							}
						}
						if (it.commandTypesSupported.contains(ApplicationCommandType.Message)) {
							this.message(it.name) {
								it.registerMessageContextCommand(this as GlobalMessageCommandCreateBuilder)
							}
						}
					}
				} else {
					logger.debug("Registering commands for subcommand group: {}", group)
					this.input(group, "$group commands") {
						commands.forEach {
							if (!it.commandTypesSupported.contains(ApplicationCommandType.ChatInput)) {
								return@forEach
							}

							this.subCommand(it.name, it.description) {

								it.registerChatInputCommand(this@subCommand)
							}
						}
					}
					commands.filter {
						it.commandTypesSupported.contains(ApplicationCommandType.User) || it.commandTypesSupported.contains(
							ApplicationCommandType.Message
						)
					}.forEach {
						if (it.commandTypesSupported.contains(ApplicationCommandType.Message)) {
							this.message(it.name) {
								it.registerMessageContextCommand(this as GlobalMessageCommandCreateBuilder)
							}
						}
						if (it.commandTypesSupported.contains(ApplicationCommandType.User)) {
							this.user(it.name) {
								it.registerUserContextCommand(this as GlobalUserCommandCreateBuilder)
							}
						}
					}
				}
			}
		}
		commandsToRegister.add(cmds)
	}
}
