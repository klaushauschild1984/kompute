import org.gradle.internal.os.OperatingSystem

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

tasks.test {
    jvmArgs("-XX:ErrorFile=${layout.buildDirectory.get().asFile.absolutePath}/hs_err_pid%p.log")
    if (OperatingSystem.current().isWindows) {
        retry {
            maxRetries.set(3)
        }
    }
}
