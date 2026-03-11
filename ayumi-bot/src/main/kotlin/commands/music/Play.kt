package dev.deathblows.app.commands.music

import dev.deathblows.app.core.commands.Command
import dev.deathblows.app.core.music.MusicManager
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.ApplicationCommandType
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.rest.builder.interaction.SubCommandBuilder
import dev.kord.rest.builder.interaction.string
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Suppress("unused")
class Play : Command(
	name = "play",
	description = "Plays a song from a given URL or search query",
	subcommandGroup = "music",
	commandTypesSupported = listOf(
		ApplicationCommandType.ChatInput,
	)
) {
	val logger: Logger = LoggerFactory.getLogger(Play::class.java)

	override fun registerChatInputCommand(builder: SubCommandBuilder) {
		return builder.string("query", "the search query or URL to play") {
			required = true
		}
	}

	@OptIn(KordVoice::class)
	override suspend fun onChatInputCommandRun(interaction: GuildChatInputCommandInteraction) {
		val player = MusicManager.getPlayerOrNull(interaction) ?: return
		println("${interaction.command.strings}, ${interaction.command.data}")
		val query = interaction.command.strings["query"] ?: return
		val tracks = MusicManager.findTracks(query, true)

		try {
			val result = player.addTracks(listOf(tracks.first()))
			interaction.respondPublic { content = "Added ${tracks.first().info.title} to the queue!" }
		} catch (error: Exception) {
			logger.error("Failed to add tracks to the queue: ${error.message}")
			interaction.respondPublic { content = "Failed to add tracks to the queue." }
			return
		}

	}
}