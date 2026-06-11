plugins {
    id("kompute.kotlin-conventions")
    id("kompute.lwjgl-conventions")
    `java-test-fixtures`
}

dependencies {
    api(project(":kompute-core"))
    implementation(libs.lwjgl)
    implementation(libs.lwjgl.glfw)
    implementation(libs.lwjgl.opengl)

    testFixturesImplementation(kotlin("test-junit5"))
}

tasks.test {
    jvmArgs("-XX:ErrorFile=${layout.buildDirectory.get().asFile.absolutePath}/hs_err_pid%p.log")
}
