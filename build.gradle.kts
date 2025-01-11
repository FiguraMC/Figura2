plugins {
	id("fabric-loom") version "1.9-SNAPSHOT"
	id("maven-publish")
}

version = "${property("mod_version")}+mc${property("mc_version")}"
group = property("mod_group").toString()
base { archivesName = "${property("mod_id")}" }

//base {
//	archivesName = project.archives_base_name
//}

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

	accessWidenerPath = rootProject.file("src/main/resources/figura.accesswidener")

	mods {
		create("figura") {
			sourceSet(sourceSets["main"])
			sourceSet(sourceSets["client"])
		}
	}
}

val cobaltBuildTools by configurations.creating {
	isCanBeConsumed = false
	isCanBeResolved = true
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${property("mc_version")}")
	mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")

	modCompileOnly("maven.modrinth:sodium:${property("sodium_version")}")
	modCompileOnly("maven.modrinth:iris:${property("iris_version")}")

	// Fabric API (specific modules we want)
	fun fabric(vararg modules: String) {
		for (module in modules)
			include(modImplementation(fabricApi.module(module, "${property("fabric_api_version")}"))!!)
	}
	fabric("fabric-api-base")

	// Cobalt (lua) dependencies:
	compileOnly("org.checkerframework:checker-qual:3.36.0")
	cobaltBuildTools(project(":cobalt-build-tools"))

	// Utils
	include(implementation("com.moulberry:mixinconstraints:1.0.1")!!)
}

tasks.processResources {
	// Text replacement mappings
	val map = mapOf("version" to project.version)
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

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()
}

