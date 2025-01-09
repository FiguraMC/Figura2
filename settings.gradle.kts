import dev.kikugie.stonecutter.settings.StonecutterSettings

pluginManagement {
	repositories {
		maven(url = "https://maven.fabricmc.net/") { name = "Fabric" }
		mavenCentral()
		gradlePluginPortal()
	}
}

plugins {
	id("dev.kikugie.stonecutter") version "0.5"
}

extensions.configure<StonecutterSettings> {
	kotlinController = true
	centralScript = "build.gradle.kts"

	create(rootProject) {
		versions("1.21")
		vcsVersion = "1.21"
	}
}

include(":cobalt-build-tools")