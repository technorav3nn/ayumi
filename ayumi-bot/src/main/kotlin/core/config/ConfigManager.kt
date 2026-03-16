package dev.deathblows.app.core.config

import com.github.michaelbull.result.runCatching
import dev.kord.common.Color
import net.mamoe.yamlkt.Yaml
import org.slf4j.LoggerFactory
import java.io.File

class ConfigException(throwable: Throwable?) : Exception("Configuration error", throwable)

object ConfigManager {
	private val logger = LoggerFactory.getLogger(ConfigManager::class.java)
	lateinit var config: Config

	fun initialize() {
		val path = getConfigPath()
		logger.info("Loading config for $path...")
		if (path == "config.dev.yml") {
			val content = this::class.java.getResource("/$path")?.readText()
				?: throw IllegalStateException("Config file not found")
			config = parseContent(content)
		} else {
			val content = File("./$path").readText()
			config = parseContent(content)
		}
		logger.info("Loaded config for $path! $config")
	}

	private fun parseContent(content: String): Config {
		val (decoded, error) = runCatching { Yaml.decodeFromString(Config.serializer(), content) }
		if (error != null) {
			throw ConfigException(error)
		}
		if (decoded == null) {
			throw ConfigException(IllegalStateException("Failed to decode config"))
		}
		return decoded
	}

	private fun getConfigPath(): String = if (getBotEnv() == BotEnv.PROD) "config.prod.yml" else "config.dev.yml"

	fun getBotEnv(): BotEnv {
		val env = System.getenv("AYUMI_ENV")
		return when (env) {
			"prod" -> BotEnv.PROD
			"dev" -> BotEnv.DEV
			else -> throw IllegalArgumentException("AYUMI_ENV must be either 'prod' or 'dev'")
		}
	}

	enum class BotEnv {
		PROD, DEV
	}

}

enum class ColorTypes {
	Info {
		override fun getColor(): Color {
			return Color(ConfigManager.config.bot.accentColor)
		}
	},
	Success {
		override fun getColor(): Color {
			return Color(ConfigManager.config.bot.successColor)
		}
	},
	Error {
		override fun getColor(): Color {
			return Color(ConfigManager.config.bot.errorColor)
		}
	},
	Warning {
		override fun getColor(): Color {
			return Color(ConfigManager.config.bot.warningColor)
		}
	};

	abstract fun getColor(): Color
}