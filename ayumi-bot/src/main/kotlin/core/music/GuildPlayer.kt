package dev.deathblows.app.core.music

import dev.arbjerg.lavalink.protocol.v4.Message.EmittedEvent.TrackEndEvent.AudioTrackEndReason
import dev.arbjerg.lavalink.protocol.v4.Track
import dev.deathblows.app.core.ACCENT_COLOR
import dev.kord.common.entity.MessageFlag
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.cache.data.UserData
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.rest.builder.message.container
import dev.kord.rest.builder.message.messageFlags
import dev.schlaubi.lavakord.audio.Link
import dev.schlaubi.lavakord.audio.TrackEndEvent
import dev.schlaubi.lavakord.audio.TrackStartEvent
import dev.schlaubi.lavakord.audio.on
import dev.schlaubi.lavakord.audio.player.Player
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory

class GuildPlayer(
	val link: Link,
	val textChannel: GuildMessageChannel
) : Player by link.player {
	private val logger = LoggerFactory.getLogger(GuildPlayer::class.java)
	private val queue: ArrayDeque<Track> = ArrayDeque()
	private val queueMutex: Mutex = Mutex()

	var currentTrackRequester: UserData? = null
	val trackRequesters: ArrayDeque<UserData> = ArrayDeque()

	init {
		on<TrackEndEvent> {
			if (reason == AudioTrackEndReason.FINISHED || reason == AudioTrackEndReason.LOAD_FAILED || reason == AudioTrackEndReason.STOPPED || reason == AudioTrackEndReason.REPLACED) {
				nextTrack()
			}
		}
		on<TrackStartEvent> {
			textChannel.createMessage {
				messageFlags {
					+MessageFlag.IsComponentsV2
				}
				container {
					accentColor = ACCENT_COLOR
					textDisplay("Now playing `${track.info.title}` by `${track.info.author}`")
					textDisplay("-# requested by ${currentTrackRequester?.username ?: "Unknown User"}")
				}
			}
		}
	}

	suspend fun enqueue(track: Track, author: User) {
		var shouldStart = false
		queueMutex.withLock {
			queue.addLast(track)
			trackRequesters.addLast(author.data)
			shouldStart = (playingTrack == null)
		}
		if (shouldStart) nextTrack()
	}

	suspend fun nextTrack(): Track? {
		val next: Pair<Track, UserData>? = queueMutex.withLock {
			if (queue.isEmpty()) return@withLock null
			val track = queue.removeFirst()
			val requester = trackRequesters.removeFirst()
			currentTrackRequester = requester
			track to requester
		}

		next ?: return null
		val (track, _) = next
		playTrack(track) { noReplace = false }
		return track
	}

	suspend fun skip() {
		this.stopTrack()
	}


}
