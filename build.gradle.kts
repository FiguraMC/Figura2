plugins {
	id("fabric-loom") version "1.6-SNAPSHOT"
	id("maven-publish")
}

//version = project.mod_version
//group = project.maven_group
//
//base {
//	archivesName = project.archives_base_name
//}

class ModData {
	val id = property("mod.id").toString()
	val name = property("mod.name").toString()
	val version = property("mod.version").toString()
	val group = property("mod.group").toString()
}
val mod = ModData()
val mcVersion = stonecutter.current.version

repositories {
	// Add repositories to retrieve artifacts from in here.
	// You should only use this when depending on other mods because
	// Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
	// See https://docs.gradle.org/current/userguide/declaring_repositories.html
	// for more information about repositories.

	mavenCentral()
	maven(url = "https://api.modrinth.com/maven") { name = "Modrinth" }
}

loom {
    splitEnvironmentSourceSets()

	accessWidenerPath = rootProject.file("src/main/resources/access_wideners/minecraft_${mcVersion}_.accesswidener")

	mods {
		create("figura") {
			sourceSet(sourceSets["main"])
			sourceSet(sourceSets["client"])
		}
	}

	runConfigs.configureEach {
		ideConfigGenerated(true)
	}
}

val cobaltBuildTools by configurations.creating {
	isCanBeConsumed = false
	isCanBeResolved = true
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${stonecutter.current.project}")
	mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:0.16.10")

	modCompileOnly("maven.modrinth:sodium:mc1.21-0.5.11")
	modCompileOnly("maven.modrinth:iris:1.7.3+1.21")

	// Fabric API (specific modules we want)
	include(modImplementation(fabricApi.module("fabric-api-base", "0.100.7+1.21"))!!)

	// Cobalt (lua) dependencies:
	compileOnly("org.checkerframework:checker-qual:3.36.0")
	cobaltBuildTools(project(":cobalt-build-tools"))

	// Utils
	include(implementation("com.moulberry:mixinconstraints:1.0.1")!!)
}

tasks.processResources {
	// Text replacement mappings
	val map = mapOf(
		"version" to mod.version,
		"mcVersion" to mcVersion
	)
	// Caching
	map.forEach(inputs::property)
	// Replace
	filesMatching("fabric.mod.json") { expand(map) }
}

// Cobalt instrumentation gradle task
val luaDirectory = project.layout.buildDirectory.dir("classes/java/client/org/figuramc/figura/script_languages/lua").get()
val instrumentForCobalt = tasks.register("InstrumentForCobalt", JavaExec::class) {
	dependsOn(tasks["compileClientJava"])

	inputs.dir(luaDirectory).withPropertyName("inputDir")
	outputs.dir(luaDirectory).withPropertyName("outputDir")

	javaLauncher = javaToolchains.launcherFor(java.toolchain)
	mainClass = "cc.tweaked.cobalt.build.MainKt"
	classpath = cobaltBuildTools

	args = listOf(luaDirectory.asFile.absolutePath)
}
tasks["compileClientJava"].finalizedBy(instrumentForCobalt)
tasks.jar { dependsOn(instrumentForCobalt) }

tasks.named("runClient") {
	dependsOn(instrumentForCobalt)
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()
}

