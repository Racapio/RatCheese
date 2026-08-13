pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/") { name = "Fabric" }
		mavenCentral()
		gradlePluginPortal()
	}
}

plugins {
	// Multi-version orchestration. Check latest at https://plugins.gradle.org/plugin/dev.kikugie.stonecutter
	id("dev.kikugie.stonecutter") version "0.9.7"
	// Lets Gradle auto-provision a matching JDK per version instead of failing when one isn't installed.
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
	create(rootProject) {
		// Add a new entry here (and a matching table in stonecutter.properties.toml)
		// to support another Minecraft version.
		versions("26.1.2", "26.2")
		// Version whose sources are the ones actually checked into git / edited in the IDE.
		vcsVersion = "26.1.2"
	}
}

rootProject.name = "ratcheese"
