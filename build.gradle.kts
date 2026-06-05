plugins {
    base
    jacoco
    id("com.saveourtool.diktat")
}

allprojects {
    version = "0.4.0-SNAPSHOT"
}

repositories {
    mavenCentral()
}

tasks.register<JacocoReport>("jacocoAggregatedReport") {
    dependsOn(
        ":kompute-core:test",
        ":kompute-core:jacocoTestReport",
        ":kompute-opengl:test",
        ":kompute-opengl:jacocoTestReport",
    )

    executionData.from(
        project(":kompute-core").layout.buildDirectory.file("jacoco/test.exec"),
        project(":kompute-opengl").layout.buildDirectory.file("jacoco/test.exec"),
    )

    sourceDirectories.from(
        project(":kompute-core").file("src/main/kotlin"),
        project(":kompute-opengl").file("src/main/kotlin"),
    )

    classDirectories.from(
        project(":kompute-core").layout.buildDirectory.dir("classes/kotlin/main"),
        project(":kompute-opengl").layout.buildDirectory.dir("classes/kotlin/main"),
    )

    reports {
        html.required = true
        csv.required = true
    }
}

tasks.check {
    dependsOn("jacocoAggregatedReport")
}
