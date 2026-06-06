plugins {
    id("kompute.kotlin-conventions")
    alias(libs.plugins.jmh)
}

dependencies {
    implementation(project(":kompute-opengl"))
    implementation("com.github.oshi:oshi-core:6.6.5")
    implementation(platform(libs.lwjgl.bom))
    runtimeOnly("org.lwjgl:lwjgl::natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-glfw::natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-opengl::natives-linux")
}
