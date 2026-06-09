plugins {
    base
    jacoco
    id("com.saveourtool.diktat")
}

allprojects {
    version = "0.8.0"
}

repositories {
    mavenCentral()
}

tasks.register<JacocoReport>("jacocoCoreReport") {
    dependsOn(
        ":kompute-core:test",
        ":kompute-core:jacocoTestReport",
    )

    executionData.from(
        project(":kompute-core").layout.buildDirectory.file("jacoco/test.exec"),
    )

    sourceDirectories.from(
        project(":kompute-core").file("src/main/kotlin"),
    )

    classDirectories.from(
        project(":kompute-core").layout.buildDirectory.dir("classes/kotlin/main"),
    )

    reports {
        html.required = true
        csv.required = true
    }
}

tasks.check {
    dependsOn("jacocoCoreReport")
}
