/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Kotlin library project to get you started.
 * For more details take a look at the 'Building Java & JVM projects' chapter in the Gradle
 * User Manual available at https://docs.gradle.org/8.1/userguide/building_java_projects.html
 */

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    kotlin("jvm")

    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    `maven-publish`
}


repositories {
    mavenCentral()
    mavenLocal()
}

val libraryVersion: String = properties["lib_version"]?.toString() ?: "latest-local"

publishing {
    repositories {
        mavenLocal()
        maven {
            url = uri("https://maven.pkg.github.com/navikt/tms-common-lib")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("gpr") {
            groupId = "no.nav.tms.common"
            artifactId = "observability"
            version = libraryVersion
            from(components["java"])
        }
    }
}

dependencies {
    api(KotlinLogging.logging)
    implementation(Ktor.Server.auth)
    implementation(Ktor.Server.authJwt)
    implementation(Ktor.Server.core)
    implementation(Ktor.Server.coreJvm)
    implementation(Ktor.Server.metricsMicrometer)
    implementation(Ktor.Server.metricsMicrometerJvm)
    implementation(Prometheus.simpleClient)
    implementation(Micrometer.registryPrometheus)

    testImplementation(Junit.engine)
    testImplementation(Junit.params)
    testImplementation(Kotest.assertionsCore)
    testImplementation(Kotest.runnerJunit)
    testImplementation(Ktor.Server.testHost)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
