package dev.deathblows.app.commands.music

import dev.deathblows.app.core.commands.Command
import dev.deathblows.app.core.music.MusicManager
import dev.kord.common.annotation.KordVoice
import dev.kord.core.behavior.interaction.respondPublic
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
		val player = MusicManager.getPlayerOrNull(interaction) ?: return
		val queue = player.queue

		logger.info("queue: $queue")
		if (queue.isEmpty()) {
			interaction.respondPublic {
				content =
					"The queue is empty! ${player.internalPlayer.playingTrack.position} ${player.internalPlayer.playingTrack.duration} ${player.internalPlayer.playingTrack.info.title}"
			}
			return
		}

		interaction.respondPublic { content = "queue: $queue" }

	}

	override fun registerChatInputCommand(builder: GlobalChatInputCreateBuilder) {}
}