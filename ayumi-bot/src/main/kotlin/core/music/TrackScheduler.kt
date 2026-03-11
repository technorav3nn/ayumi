package dev.deathblows.app.core.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class TrackScheduler(
	private val guildPlayer: GuildPlayer
) : AudioEventAdapter() {
	private val logger = LoggerFactory.getLogger(TrackScheduler::class.java)

	override fun onTrackStart(player: AudioPlayer?, track: AudioTrack?) {

	}

	override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason?) {
		logger.info("Track end $endReason")
		if (endReason?.mayStartNext == true) {
			val nextTrack = guildPlayer.queue.poll()
			if (nextTrack != null) {
				runBlocking { guildPlayer.nextTrack() }
			}
		}
	}

	override fun onTrackStuck(player: AudioPlayer?, track: AudioTrack?, thresholdMs: Long) {}

	override fun onTrackException(player: AudioPlayer?, track: AudioTrack?, exception: FriendlyException?) {}

}