package dev.deathblows.app.commands.music

import com.github.michaelbull.result.annotation.UnsafeResultErrorAccess
import dev.deathblows.app.core.ACCENT_COLOR
import dev.deathblows.app.core.commands.Command
import dev.deathblows.app.core.music.MusicManager
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.MessageFlag
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.rest.builder.message.container
import dev.kord.rest.builder.message.messageFlags
import org.slf4j.LoggerFactory

@OptIn(KordVoice::class)
class Skip : Command(
	name = "skip",
	description = "skips the current song!",
	subcommandGroup = "music"
) {
	private val logger = LoggerFactory.getLogger(Skip::class.java)

	@OptIn(UnsafeResultErrorAccess::class)
	override suspend fun onChatInputCommandRun(interaction: GuildChatInputCommandInteraction) {
		val player = MusicManager.getPlayer(interaction) ?: run {
			logger.debug("No player found for interaction: {}", interaction)
			return
		}
		player.skip()

		interaction.respondPublic {
			messageFlags {
				+MessageFlag.IsComponentsV2
			}
			container {
				accentColor = ACCENT_COLOR
				textDisplay("skipped the current song!")
			}
		}
	}

}