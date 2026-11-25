plugins {
    `maven-publish`
    `java-library`
    kotlin("jvm")
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api(libs.kotlinLogging)
    implementation(libs.logback.classic)
    implementation(libs.logback.core)
    implementation(libs.logstash.logbackEncoder)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.jackson3.core)
    runtimeOnly(libs.jackson3.databind)

    testImplementation(libs.junit.api)
    testImplementation(libs.kotest.assertionsCore)
    testImplementation(libs.kotest.assertionsShared)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.launcher)
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
            artifactId = "team-logger"
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

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
