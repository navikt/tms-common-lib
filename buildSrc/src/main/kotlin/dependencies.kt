interface DependencyGroup {
    val groupId: String? get() = null
    val version: String? get() = null

    fun dependency(name: String, groupId: String? = this.groupId, version: String? = this.version): String {
        requireNotNull(groupId)
        requireNotNull(version)

        return "$groupId:$name:$version"
    }
}

object Prometheus: DependencyGroup {
    override val version = "0.16.0"
    override val groupId = "io.prometheus"

    val simpleClient = dependency("simpleclient_common")
}

object Micrometer: DependencyGroup {
    override val version = "1.13.3"
    override val groupId = "io.micrometer"

    val registryPrometheus = dependency("micrometer-registry-prometheus")
}

object Kotlin {
    const val version = "1.9.23"
}

object Kotest: DependencyGroup {
    override val groupId = "io.kotest"
    override val version = "5.9.1"

    val runnerJunit = dependency("kotest-runner-junit5")
    val assertionsCore = dependency("kotest-assertions-core")
    val extensions = dependency("kotest-extensions")
}

object Ktor {
    val version get() = "3.0.1"
    val groupId get() = "io.ktor"

    object Server: DependencyGroup {
        override val groupId get() = Ktor.groupId
        override val version get() = Ktor.version

        val core get() = dependency("ktor-server-core")
        val coreJvm = dependency("ktor-server-core-jvm")
        val netty get() = dependency("ktor-server-netty")
        val defaultHeaders get() = dependency("ktor-server-default-headers")
        val metricsMicrometer get() = dependency("ktor-server-metrics-micrometer")
        val metricsMicrometerJvm = dependency("ktor-server-metrics-micrometer-jvm")
        val auth get() = dependency("ktor-server-auth")
        val authJwt get() = dependency("ktor-server-auth-jwt")
        val contentNegotiation get() = dependency("ktor-server-content-negotiation")
        val statusPages get() = dependency("ktor-server-status-pages")
        val testHost get() = dependency("ktor-server-test-host")
    }

    object Client: DependencyGroup {
        override val groupId get() = Ktor.groupId
        override val version get() = Ktor.version

        val core get() = dependency("ktor-client-core")
        val apache get() = dependency("ktor-client-apache")
        val contentNegotiation get() = dependency("ktor-client-content-negotiation")
    }

    object Serialization: DependencyGroup {
        override val groupId get() = Ktor.groupId
        override val version get() = Ktor.version

        val jackson get() = dependency("ktor-serialization-jackson")
    }
}

object KotlinLogging: DependencyGroup {
    override val groupId = "io.github.oshai"
    override val version = "7.0.0"

    val logging = dependency("kotlin-logging")
}


object Logback: DependencyGroup {
    override val version = "1.5.12"
    val classic = "ch.qos.logback:logback-classic:$version"
}

object Mockk: DependencyGroup {
    override val version = "1.13.13"
    val mockk = "io.mockk:mockk:$version"
}

object Jackson: DependencyGroup {
    override val version get() = "2.17.2"

    val datatypeJsr310 get() = dependency("jackson-datatype-jsr310", groupId = "com.fasterxml.jackson.datatype")
    val moduleKotlin get() = dependency("jackson-module-kotlin", groupId = "com.fasterxml.jackson.module")
}

object Junit: DependencyGroup {
    override val version = "5.11.0"
    override val groupId = "org.junit.jupiter"

    val api = dependency("junit-jupiter-api")
    val engine = dependency("junit-jupiter-engine")
    val params = dependency("junit-jupiter-params")
}

object Kotlinx: DependencyGroup {
    override val groupId = "org.jetbrains.kotlinx"

    val coroutines = dependency("kotlinx-coroutines-core", version = "1.8.1")
}
