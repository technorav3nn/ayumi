package dev.deathblows.app.core.utils

fun String.truncate(maxLength: Int, suffix: String = "..."): String {
	if (this.length <= maxLength) return this
	return this.substring(0, maxLength - suffix.length) + suffix
}