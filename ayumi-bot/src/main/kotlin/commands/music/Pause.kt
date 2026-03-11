package dev.deathblows.app.commands.music

import dev.deathblows.app.core.commands.Command
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder

class Pause : Command(
	name = "pause",
	description = "Pauses the currently playing song",
	subcommandGroup = "music"
) {
	override fun registerChatInputCommand(builder: GlobalChatInputCreateBuilder) {}
}