plugins {
	id("buildsrc.convention.kotlin-jvm")

	// Apply the Application plugin to add support for building an executable JVM application.
	application
	alias(libs.plugins.kotlinPluginSerialization)
}

repositories {
	mavenCentral()
	maven("https://snapshots.kord.dev")
	maven("https://m2.dv8tion.net/releases")
	maven("https://maven.lavalink.dev/releases")
	maven("https://maven.arbjerg.dev/snapshots")
	maven("https://jitpack.io")
	maven("https://oss.sonatype.org/content/repositories/snapshots")
	maven("https://schlaubi.jfrog.io/artifactory/lavakord")
}

dependencies {
	// shared
	implementation(project(":ayumi-common"))

	// discord
	implementation("dev.kord:kord-voice:0.18.0")
	implementation("dev.kord:kord-core:0.18.0")
	implementation("dev.kord:kord-core-voice:0.18.0")
	implementation("dev.schlaubi.lavakord:kord:9.2.0")
	implementation("dev.schlaubi.lavakord:lavasrc:9.2.0")

	// logging
	implementation("ch.qos.logback:logback-classic:1.5.32")

	// misc
	implementation("io.github.classgraph:classgraph:4.8.184")
	implementation("com.michael-bull.kotlin-result:kotlin-result:2.1.0")
	implementation("net.mamoe.yamlkt:yamlkt:0.13.0")
}

application {
	mainClass = "dev.deathblows.app.AppKt"
}
