plugins {
    id("kompute.kotlin-conventions")
    alias(libs.plugins.jmh)
}

dependencies {
    implementation(project(":kompute-opengl"))
    implementation("com.github.oshi:oshi-core:6.6.5")
    runtimeOnly("org.lwjgl:lwjgl:${libs.versions.lwjgl.get()}:natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-glfw:${libs.versions.lwjgl.get()}:natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-opengl:${libs.versions.lwjgl.get()}:natives-linux")
}
