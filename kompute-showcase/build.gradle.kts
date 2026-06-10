plugins {
    id("kompute.kotlin-conventions")
}

dependencies {
    implementation(project(":kompute-core"))
    runtimeOnly(libs.logback)
    runtimeOnly(project(":kompute-opengl"))
    runtimeOnly(platform(libs.lwjgl.bom))
    runtimeOnly("org.lwjgl:lwjgl::natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-glfw::natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-opengl::natives-linux")
}
