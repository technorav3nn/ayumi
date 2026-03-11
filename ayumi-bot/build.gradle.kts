plugins {
	id("buildsrc.convention.kotlin-jvm")

	// Apply the Application plugin to add support for building an executable JVM application.
	application
}

repositories {
	mavenCentral()
	maven("https://snapshots.kord.dev")
	maven("https://m2.dv8tion.net/releases")
	maven("https://maven.lavalink.dev/releases")
	maven("https://jitpack.io")
}

dependencies {
	// shared
	implementation(project(":ayumi-common"))

	// discord
	implementation("dev.kord:kord-voice:0.18.0")
	implementation("dev.kord:kord-core:0.18.0")
	implementation("dev.kord:kord-core-voice:0.18.0")
	implementation("dev.arbjerg:lavaplayer:2.2.6")
	implementation("dev.lavalink.youtube:v2:1.18.0")

	// logging
	implementation("ch.qos.logback:logback-classic:1.5.32")

	// misc
	implementation("io.github.classgraph:classgraph:4.8.184")
}

application {
	mainClass = "dev.deathblows.app.AppKt"
}
