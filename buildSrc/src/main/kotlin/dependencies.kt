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
    override val version = "0.9.0"
    override val groupId = "io.prometheus"

    val simpleClient = dependency("simpleclient_common")
}

object Micrometer: DependencyGroup {
    override val version = "1.7.0"
    override val groupId = "io.micrometer"

    val registryPrometheus = dependency("micrometer-registry-prometheus")
}

object Caffeine: DependencyGroup {
    override val version = "3.0.0"
    override val groupId = "com.github.ben-manes.caffeine"

    val caffeine = dependency("caffeine")
}
object Kluent: DependencyGroup {
    override val version = "1.68"
    override val groupId = "org.amshove.kluent"

    val kluent = dependency("kluent")
}

object Kotlin {
    const val version = "1.8.21"
}

object Kotest: DependencyGroup {
    override val groupId = "io.kotest"
    override val version = "4.3.1"

    val runnerJunit = dependency("kotest-runner-junit5")
    val assertionsCore = dependency("kotest-assertions-core")
    val extensions = dependency("kotest-extensions")
}

object KtorServer: DependencyGroup {
    override val version = "2.3.2"
    override val groupId = "io.ktor"

    val coreJvm = dependency("ktor-server-core-jvm")
    val metricsMicrometerJvm = dependency("ktor-server-metrics-micrometer-jvm")
    val core = dependency("ktor-server-core")
    val auth = dependency("ktor-server-auth")
    val authJwt = dependency("ktor-server-auth-jwt")
    val metricsMicrometer = dependency("ktor-server-metrics-micrometer")
    val testHost = dependency("ktor-server-test-host")
}

object KotlinLogging: DependencyGroup {
    override val groupId = "io.github.oshai"
    override val version = "5.0.2"

    val logging = dependency("kotlin-logging")
}


object Logback: DependencyGroup {
    override val version = "1.4.11"
    val classic = "ch.qos.logback:logback-classic:$version"
}

object Mockk: DependencyGroup {
    override val version = "1.12.3"
    val mockk = "io.mockk:mockk:$version"
}

object Nimbusds: DependencyGroup {
    override val version = "9.19"
    override val groupId = "com.nimbusds"

    val joseJwt = dependency("nimbus-jose-jwt")
    val oauth2OidcSdk =  dependency("oauth2-oidc-sdk")
}


object Junit: DependencyGroup {
    override val version = "5.9.3"
    override val groupId = "org.junit.jupiter"

    val api = dependency("junit-jupiter-api")
    val engine = dependency("junit-jupiter-engine")
    val params = dependency("junit-jupiter-params")
}
object Jjwt: DependencyGroup {
    override val version = "0.11.2"
    override val groupId = "io.jsonwebtoken"

    val api = dependency("jjwt-api")
    val impl = dependency("jjwt-impl")
    val jackson = dependency("jjwt-jackson")
}

object Kotlinx: DependencyGroup {
    override val groupId = "org.jetbrains.kotlinx"

    val coroutines = "$groupId:kotlinx-coroutines-core:1.3.9"
    val datetime = "$groupId:kotlinx-datetime:0.3.2"
}


object Logstash: DependencyGroup {
    override val version = "6.4"
    val logbackEncoder = "net.logstash.logback:logstash-logback-encoder:$version"
}
