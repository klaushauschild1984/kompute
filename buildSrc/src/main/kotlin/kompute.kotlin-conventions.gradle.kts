plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    id("jacoco")
}

group = "de.hauschild.kompute"

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.18")
}

sourceSets {
    test {
        resources {
            srcDir(rootProject.file("src/test/resources"))
        }
    }
}

tasks.jar {
    manifest {
        attributes["Implementation-Version"] = project.version
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        html.required = true
        csv.required = true
    }
}

ktlint {
    version.set("1.4.1")
    filter {
        exclude("**/generated/**")
    }
}
