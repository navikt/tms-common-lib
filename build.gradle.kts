import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    kotlin("jvm") version Kotlin.version
}

repositories {
    mavenCentral()
    mavenLocal()
}

tasks {
    jar {
        enabled = false
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

buildscript {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }

    dependencies {
        classpath("com.github.ben-manes:gradle-versions-plugin:+")
    }
}
apply(plugin = "com.github.ben-manes.versions")

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {

    // optional parameters
    checkForGradleUpdate = true
    outputFormatter = "json"
    outputDir = "build/dependencyUpdates"
    reportfileName = "dependencies"

    rejectVersionIf {
        isNonStable(candidate.version)
    }
}
