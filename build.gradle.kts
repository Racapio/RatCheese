plugins {
	id("net.fabricmc.fabric-loom") version "1.17.19"
}

// DO NOT set group here - Stonecutter needs each version subproject to keep its own.
version = "${property("mod.version")}+${stonecutter.current.version}"
base.archivesName.set(property("mod.id") as String)

repositories {
	// Mod Menu integration (config screen entry)
	exclusiveContent {
		forRepository { maven("https://maven.terraformersmc.com/releases") }
		filter { includeGroup("com.terraformersmc") }
	}
}

loom {
	mods {
		create(property("mod.id") as String) {
			sourceSet(sourceSets.main.get())
		}
	}
}

dependencies {
	minecraft("com.mojang:minecraft:${stonecutter.current.version}")
	// No explicit mappings() call: Loom applies Mojang's official mappings automatically
	// for these versions, same as the pre-Stonecutter build.

	implementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
	implementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
	implementation("com.terraformersmc:modmenu:${property("deps.mod_menu")}")
}

tasks.processResources {
	val props = mapOf(
		"id" to project.property("mod.id"),
		"name" to project.property("mod.name"),
		"version" to project.version,
		"minecraft" to project.property("mod.mc_compat")
	)
	inputs.properties(props)
	filesMatching("fabric.mod.json") { expand(props) }
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(25)
	options.encoding = "UTF-8"
}

java {
	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

// `gradlew :26.1.2:buildAndCollect :26.2:buildAndCollect` copies every version's jar here.
tasks.register<Copy>("buildAndCollect") {
	group = "build"
	description = "Builds the mod jar and copies it to build/libs/<mod version>/"

	dependsOn("build")
	from(tasks.named("jar"))
	into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod.version")}"))
}

