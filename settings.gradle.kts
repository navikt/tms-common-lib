rootProject.name = "tms-common-lib"
include("metrics", "utils", "observability", "kubernetes", "team-logger", "postgres")

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.21" apply false
    id("com.autonomousapps.build-health") version "3.20.4"
}
