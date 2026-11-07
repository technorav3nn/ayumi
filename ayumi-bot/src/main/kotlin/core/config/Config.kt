package dev.deathblows.app.core.config

import kotlinx.serialization.Serializable

@Serializable
data class Config(
	val logLevel: String,
	val bot: BotConfig,
	val lavalink: LavalinkConfig
) {
	@Serializable
	data class BotConfig(
		val token: String,
		val devGuildId: Long? = null,

		val accentColor: Int,
		val successColor: Int,
		val errorColor: Int,
		val warningColor: Int,
	)

	@Serializable
	data class LavalinkConfig(
		val host: String,
		val password: String,
	)
}
