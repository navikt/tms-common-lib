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
    implementation(KtorServer.auth)
    implementation(KtorServer.authJwt)
    implementation(KtorServer.core)
    implementation(KtorServer.coreJvm)
    implementation(KtorServer.metricsMicrometer)
    implementation(KtorServer.metricsMicrometerJvm)
    implementation(Prometheus.simpleClient)
    implementation(Micrometer.registryPrometheus)

    testImplementation(Junit.engine)
    testImplementation(Junit.params)
    testImplementation(Kotest.assertionsCore)
    testImplementation(Kotest.runnerJunit)
    testImplementation(KtorServer.testHost)
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
