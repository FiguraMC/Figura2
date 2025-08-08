pluginManagement {
	repositories {
		maven(url = "https://maven.fabricmc.net/") { name = "Fabric" }
		mavenCentral()
		gradlePluginPortal()
	}
}

include(":cobalt-build-tools")
include(":figura-comptime")