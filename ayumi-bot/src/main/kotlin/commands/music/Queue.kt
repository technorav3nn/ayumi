package dev.deathblows.app.commands.music

import dev.deathblows.app.core.commands.Command
import dev.kord.common.annotation.KordVoice
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import org.slf4j.LoggerFactory

class Queue : Command(
	name = "queue",
	description = "retrieves the queue!",
	subcommandGroup = "music"
) {
	val logger = LoggerFactory.getLogger(Queue::class.java)

	@OptIn(KordVoice::class)
	override suspend fun onChatInputCommandRun(interaction: GuildChatInputCommandInteraction) {

	}

	override fun registerChatInputCommand(builder: GlobalChatInputCreateBuilder) {}
}