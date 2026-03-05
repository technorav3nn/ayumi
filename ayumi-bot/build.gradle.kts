plugins {
	id("buildsrc.convention.kotlin-jvm")
	// Apply the Application plugin to add support for building an executable JVM application.
	application
}

repositories {
	maven("https://snapshots.kord.dev")
	mavenCentral()
}

dependencies {
	// shared
	implementation(project(":ayumi-common"))

	// discord
	implementation("me.jakejmattson:DiscordKt:0.26.0")

	// logging
	implementation("ch.qos.logback:logback-classic:1.5.32")
}

application {
	mainClass = "dev.deathblows.app.AppKt"
}
