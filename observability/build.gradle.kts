import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest

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

            val sourcesJar by tasks.registering(Jar::class) {
                archiveClassifier.set("sources")
                from(sourceSets.main.get().allSource)
            }

            artifact(sourcesJar)
        }
    }
}

dependencies {
    compileOnly(KotlinLogging.logging)
    compileOnly(Ktor.Server.core)
    implementation(Ktor.Server.metricsMicrometer)
    implementation(Prometheus.exporterCommon)
    implementation(Prometheus.metricsCore)
    implementation(Micrometer.registryPrometheus)
    implementation(Logback.classic)

    testImplementation(JunitPlatform.launcher)
    testImplementation(JunitJupiter.params)
    testImplementation(JunitJupiter.api)
    testImplementation(Kotest.assertionsCore)
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
