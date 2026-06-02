plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

group = "de.hauschild.kompute"

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

ktlint {
    version.set("1.4.1")
}
