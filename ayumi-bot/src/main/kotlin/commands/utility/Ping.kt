package dev.deathblows.app.commands.utility

import dev.deathblows.app.core.commands.Command
import dev.kord.common.entity.ApplicationCommandType
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.GlobalUserCommandCreateBuilder
import dev.kord.rest.builder.interaction.boolean

@Suppress("unused")
class Ping : Command(
	name = "ping",
	description = "Checks the bot's latency",
	commandTypesSupported = listOf(
		ApplicationCommandType.ChatInput,
		ApplicationCommandType.User,
	)
) {
	override fun registerChatInputCommand(builder: GlobalChatInputCreateBuilder) {
		builder.boolean("ephemeral", "Whether the response should be ephemeral (only visible to you)")
	}

	override fun registerUserContextCommand(builder: GlobalUserCommandCreateBuilder) {}
}