plugins {
    id("kompute.kotlin-conventions")
}

dependencies {
    implementation(project(":kompute-core"))
    runtimeOnly("ch.qos.logback:logback-classic:1.5.18")
    runtimeOnly(project(":kompute-opengl"))
    runtimeOnly(platform(libs.lwjgl.bom))
    runtimeOnly("org.lwjgl:lwjgl::natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-glfw::natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-opengl::natives-linux")
}
