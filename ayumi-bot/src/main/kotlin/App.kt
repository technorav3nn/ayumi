package dev.deathblows.app

import dev.kord.common.entity.ALL
import dev.kord.common.entity.Permissions
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.NON_PRIVILEGED
import me.jakejmattson.discordkt.dsl.bot
import org.slf4j.LoggerFactory
import java.awt.Color

fun main(args: Array<String>) {
	val token = args.firstOrNull() ?: "MTQ3OTE1OTQyNDcwOTgyNDU5Mw.G4YTOp.NWC6c1qiIDfaslGI9QKKsLGoVotj-_v9Z41t-o"
	val logger = LoggerFactory.getLogger("App")

	bot(token) {
		configure {
			theme = Color(235, 52, 103)
			intents = Intents.NON_PRIVILEGED + Intents(Intent.Guilds)
			defaultPermissions = Permissions.ALL
		}

		onStart {
			logger.info("Bot is ready!")
		}
	}
}
