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
    implementation(Ktor.Server.core)
    implementation(Ktor.Client.core)
    implementation(Ktor.Server.statusPages)
    implementation(Kotlin.reflect)
    implementation(KotlinLogging.logging)
    implementation(Logback.classic)
    implementation(Logstash.logbackEncoder)


    testImplementation(Junit.engine)
    testImplementation(Kotest.assertionsCore)
    testImplementation(Kotest.runnerJunit)
    testImplementation(KtorTest.serverTestHost)
    testImplementation(Mockk.mockk)
    testImplementation(project(":test-utils"))

}

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
            artifactId = "errorhandling"
            version = properties["lib_version"]?.toString() ?: "latest-local"
            from(components["java"])
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}