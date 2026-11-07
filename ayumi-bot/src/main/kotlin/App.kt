package dev.deathblows.app

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import dev.kord.common.annotation.KordVoice
import org.slf4j.LoggerFactory

@KordVoice
suspend fun main(args: Array<String>) {
	val logLevel = System.getenv("LOG_LEVEL") ?: "INFO"
	val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext?
	val rootLogger: Logger =
		loggerContext?.getLogger(Logger.ROOT_LOGGER_NAME) ?: throw IllegalStateException("Root logger not found")
	rootLogger.setLevel(Level.toLevel(logLevel.uppercase()))

	val logger = LoggerFactory.getLogger("App_EntryPoint")

	val ayumi = Ayumi()

	try {
		ayumi.start()
	} catch (e: Exception) {
		logger.error("An error occurred while starting Ayumi.", e)
	}
}
