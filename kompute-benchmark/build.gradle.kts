plugins {
    id("kompute.kotlin-conventions")
    alias(libs.plugins.jmh)
}

dependencies {
    implementation(project(":kompute-opengl"))
    implementation(libs.oshi)
    implementation(platform(libs.lwjgl.bom))
    runtimeOnly("org.lwjgl:lwjgl::natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-glfw::natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-opengl::natives-linux")
}
