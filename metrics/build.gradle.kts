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

            val sourcesJar by tasks.creating(Jar::class) {
                archiveClassifier.set("sources")
                from(sourceSets.main.get().allSource)
            }

            artifact(sourcesJar)
        }
    }
}

dependencies {
    implementation(KotlinLogging.logging)
    implementation(Ktor.Server.auth)
    implementation(Ktor.Server.authJwt)
    implementation(Ktor.Server.core)
    implementation(Ktor.Server.coreJvm)
    implementation(Ktor.Server.metricsMicrometer)
    implementation(Ktor.Server.metricsMicrometerJvm)
    implementation(Prometheus.exporterCommon)
    implementation(Prometheus.metricsCore)
    implementation(Micrometer.registryPrometheus)

    testImplementation(Junit.engine)
    testImplementation(Junit.params)
    testImplementation(Kotest.assertionsCore)
    testImplementation(Kotest.runnerJunit)
    testImplementation(Ktor.Server.testHost)
}

// Apply a specific Java toolchain to ease working on different environments.
kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
