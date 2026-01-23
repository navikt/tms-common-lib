interface DependencyGroup {
    val groupId: String? get() = null
    val version: String? get() = null

    fun dependency(name: String, groupId: String? = this.groupId, version: String? = this.version): String {
        requireNotNull(groupId)
        requireNotNull(version)

        return "$groupId:$name:$version"
    }
}

object Hikari: DependencyGroup {
    override val groupId get() = "com.zaxxer"
    override val version get() = "7.0.2"

    val cp get() = dependency("HikariCP")
}

object JacksonDatatype: DependencyGroup {
    override val version get() = "2.20.1"

    val datatypeJsr310 get() = dependency("jackson-datatype-jsr310", groupId = "com.fasterxml.jackson.datatype")
    val moduleKotlin get() = dependency("jackson-module-kotlin", groupId = "com.fasterxml.jackson.module")
}

object JunitJupiter: DependencyGroup {
    override val groupId get() = "org.junit.jupiter"
    override val version get() = "6.0.2"

    val api get() = dependency("junit-jupiter-api")
    val engine get() = dependency("junit-jupiter-engine")
    val params get() = dependency("junit-jupiter-params")
}

object JunitPlatform: DependencyGroup {
    override val groupId get() = "org.junit.platform"
    override val version get() = "6.0.2"

    val launcher get() = dependency("junit-platform-launcher")
}

object Kotest: DependencyGroup {
    override val groupId get() = "io.kotest"
    override val version get() = "6.0.7"

    val assertionsCore get() = dependency("kotest-assertions-core")
    val extensions get() = dependency("kotest-extensions")
}

object Kotlin: DependencyGroup {
    override val groupId get() = "org.jetbrains.kotlin"
    override val version get() = "2.3.0"
}

object KotlinLogging: DependencyGroup {
    override val groupId get() = "io.github.oshai"
    override val version get() = "7.0.14"

    val logging get() = dependency("kotlin-logging")
}

object Kotlinx: DependencyGroup {
    override val groupId get() = "org.jetbrains.kotlinx"

    val coroutines get() = dependency("kotlinx-coroutines-core", version = "1.10.2")
}

object KotliQuery: DependencyGroup {
    override val groupId get() = "com.github.seratch"
    override val version get() = "1.9.1"

    val kotliquery get() = dependency("kotliquery")
}

object Ktor {
    val version get() = "3.3.3"
    val groupId get() = "io.ktor"

    object Server: DependencyGroup {
        override val groupId get() = Ktor.groupId
        override val version get() = Ktor.version

        val core get() = dependency("ktor-server-core")
        val netty get() = dependency("ktor-server-netty")
        val defaultHeaders get() = dependency("ktor-server-default-headers")
        val metricsMicrometer get() = dependency("ktor-server-metrics-micrometer")
        val auth get() = dependency("ktor-server-auth")
        val authJwt get() = dependency("ktor-server-auth-jwt")
        val contentNegotiation get() = dependency("ktor-server-content-negotiation")
        val statusPages get() = dependency("ktor-server-status-pages")
        val htmlDsl get() = dependency("ktor-server-html-builder")
        val cors get() = dependency("ktor-server-cors")
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

        val kotlinX get() = dependency("ktor-serialization-kotlinx-json")
        val jackson get() = dependency("ktor-serialization-jackson")
    }

    object Test: DependencyGroup {
        override val groupId get() = Ktor.groupId
        override val version get() = Ktor.version

        val clientMock get() = dependency("ktor-client-mock")
        val serverTestHost get() = dependency("ktor-server-test-host")
    }
}

object Logback: DependencyGroup {
    override val version = "1.5.24"
    val classic = "ch.qos.logback:logback-classic:$version"
}

object Logstash: DependencyGroup {
    override val groupId get() = "net.logstash.logback"
    override val version get() = "9.0"

    val logbackEncoder get() = dependency("logstash-logback-encoder")
}

object Micrometer: DependencyGroup {
    override val groupId get() = "io.micrometer"
    override val version get() = "1.16.1"

    val registryPrometheus get() = dependency("micrometer-registry-prometheus")
}

object Mockk: DependencyGroup {
    override val groupId get() = "io.mockk"
    override val version get() = "1.14.7"

    val mockk get() = dependency("mockk")
}

object Postgresql: DependencyGroup {
    override val groupId get() = "org.postgresql"
    override val version get() = "42.7.8"

    val postgresql get() = dependency("postgresql")
}


object Prometheus: DependencyGroup {
    override val version get() = "1.4.3"
    override val groupId get() = "io.prometheus"

    val metricsCore get() = dependency("prometheus-metrics-core")
    val exporterCommon get() = dependency("prometheus-metrics-exporter-common")
}

object TestContainers: DependencyGroup {
    override val version get() = "2.0.3"
    override val groupId get() = "org.testcontainers"

    val postgresql get() = dependency("testcontainers-postgresql")
}
