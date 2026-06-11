plugins {
    id("kompute.kotlin-conventions")
    id("kompute.lwjgl-conventions")
    alias(libs.plugins.testRetry)
}

dependencies {
    api(project(":kompute-core"))
    implementation(libs.lwjgl)
    implementation(libs.lwjgl.glfw)
    implementation(libs.lwjgl.opengl)
}

tasks.withType<Test> {
    jvmArgs("-XX:ErrorFile=${layout.buildDirectory.get().asFile.absolutePath}/hs_err_pid%p.log")
    retry {
        maxRetries.set(3)
    }
}
