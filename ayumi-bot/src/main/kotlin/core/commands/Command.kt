package dev.deathblows.app.core.commands

import dev.kord.common.entity.ApplicationCommandType
import dev.kord.core.entity.interaction.GuildAutoCompleteInteraction
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.core.entity.interaction.GuildComponentInteraction
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.GlobalMessageCommandCreateBuilder
import dev.kord.rest.builder.interaction.GlobalUserCommandCreateBuilder
import dev.kord.rest.builder.interaction.SubCommandBuilder

interface CommandHandler {
	fun registerChatInputCommand(builder: GlobalChatInputCreateBuilder) {}
	fun registerChatInputCommand(builder: SubCommandBuilder) {}
	fun registerMessageContextCommand(builder: GlobalMessageCommandCreateBuilder) {}
	fun registerUserContextCommand(builder: GlobalUserCommandCreateBuilder) {}

	suspend fun onChatInputCommandRun(interaction: GuildChatInputCommandInteraction) {}
	suspend fun onExternalInteractionRun(interaction: GuildComponentInteraction) {}
	suspend fun onAutocompleteRun(interaction: GuildAutoCompleteInteraction) {}
}

open class Command(
	val name: String,
	val description: String,
	val subcommandGroup: String? = null,
	val commandTypesSupported: List<ApplicationCommandType> = listOf(ApplicationCommandType.ChatInput),
	val componentCustomIds: List<String> = emptyList(),
	val supportsAutocomplete: Boolean = false
) : CommandHandler