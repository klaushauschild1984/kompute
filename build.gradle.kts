
plugins {
    base
    jacoco
}

allprojects {
    version = "1.0.0-SNAPSHOT"
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

    executionData.from(fileTree(rootDir) {
        include("**/build/jacoco/test.exec")
        exclude("kompute-benchmark/**", "kompute-kotlin/**")
    })

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
