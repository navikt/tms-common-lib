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

val libraryVersion: String = providers.gradleProperty("lib_version")
    .orElse("latest-local")
    .get()

publishing {
    repositories{
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
            artifactId = "metrics"
            version = libraryVersion
            from(components["java"])

            val sourcesJar = tasks.register<Jar>("sources") {
                archiveClassifier.set("sources")
                from(sourceSets.main.get().allSource)
            }

            artifact(sourcesJar)
        }
    }
}

dependencies {
    api(libs.ktor.events)
    api(libs.ktor.http)
    api(libs.ktor.server.core)
    api(libs.ktor.utils)
    api(libs.micrometer.registryPrometheus)
    implementation(libs.auth0.javaJwt)
    implementation(libs.kotlinLogging)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.authJwt)
    implementation(libs.ktor.server.metricsMicrometer)
    implementation(libs.micrometer.core)
    implementation(libs.prometheus.metricsCore)
    implementation(libs.prometheus.metricsExpositionTextformats)
    implementation(libs.prometheus.metricsModel)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)

    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.kotest.assertionsCore)
    testImplementation(libs.kotest.assertionsShared)
    testImplementation(libs.junit.commons)
    testImplementation(libs.kotlinx.coroutines)
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.test.server)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.launcher)
}

// Apply a specific Java toolchain to ease working on different environments.
kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
