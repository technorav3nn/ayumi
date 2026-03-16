package dev.deathblows.app.core.utils

import dev.deathblows.app.core.config.ColorTypes
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.ActionInteractionBehavior
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.MessageInteractionResponseBehavior
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.rest.builder.component.ContainerBuilder
import dev.kord.rest.builder.message.container

suspend inline fun ActionInteractionBehavior.respondContainer(
	text: String,
	color: ColorTypes = ColorTypes.Info,
	noinline builder: (ContainerBuilder.() -> Unit)?
): MessageInteractionResponseBehavior {
	return respondPublic {
		container {
			accentColor = color.getColor()
			textDisplay(text)
			builder?.invoke(this)
		}
	}
}

suspend inline fun ActionInteractionBehavior.respondContainerEphemeral(
	text: String,
	color: ColorTypes = ColorTypes.Info,
	noinline builder: (ContainerBuilder.() -> Unit)?
): EphemeralMessageInteractionResponseBehavior {
	// reason why we don't use message flags: Kord has separate types
	// for ephemeral messages, which I'm not sure if they're super different or not
	return respondEphemeral {
		container {
			accentColor = color.getColor()
			textDisplay(text)
			builder?.invoke(this)
		}
	}
}

suspend inline fun GuildMessageChannel.createContainerMessage(
	text: String,
	color: ColorTypes = ColorTypes.Info,
	noinline builder: (ContainerBuilder.() -> Unit)?
): Message {
	// reason why we don't use message flags: Kord has separate types
	// for ephemeral messages, which I'm not sure if they're super different or not
	return createMessage {
		container {
			accentColor = color.getColor()
			textDisplay(text)
			builder?.invoke(this)
		}
	}
}
