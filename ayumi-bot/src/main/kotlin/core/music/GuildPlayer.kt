package dev.deathblows.app.core.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.kord.common.annotation.KordVoice
import dev.kord.core.behavior.channel.BaseVoiceChannelBehavior
import dev.kord.core.behavior.channel.connect
import dev.kord.voice.AudioFrame
import dev.kord.voice.VoiceConnection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.slf4j.LoggerFactory
import java.util.*

/**
 * A Guild Player instance
 */
@OptIn(KordVoice::class, ExperimentalCoroutinesApi::class)
class GuildPlayer(
	val internalPlayer: AudioPlayer,
) {
	var queue: ArrayDeque<AudioTrack?> = ArrayDeque()

	private val logger = LoggerFactory.getLogger(GuildPlayer::class.java)
	private var connection: VoiceConnection? = null

	suspend fun disconnect() {
		connection?.disconnect()
		connection = null
	}

	suspend fun connect(channel: BaseVoiceChannelBehavior) {
		connection = channel.connect {
			audioProvider { AudioFrame.fromData(internalPlayer.provide()?.data) }
			selfDeaf = true
		}
	}


	fun addTracks(tracks: List<AudioTrack>, addToFront: Boolean = false) {
		if (addToFront) {
			tracks.forEach { queue.addFirst(it) }
		} else {
			queue.addAll(tracks)
		}
		if (internalPlayer.playingTrack == null) {
			nextTrack()
		}
	}

	fun nextTrack() {
		val track = queue.poll()
		logger.info(track?.info?.title)
		if (track == null) {
			return
		}

		internalPlayer.playTrack(track)
	}
}