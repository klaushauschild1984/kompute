import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.internal.os.OperatingSystem

val libs = the<LibrariesForLibs>()

val operatingSystem:OperatingSystem = OperatingSystem.current()
val lwjglNatives = when {
    operatingSystem.isWindows -> "natives-windows"
    operatingSystem.isLinux   -> "natives-linux"
    operatingSystem.isMacOsX ->
        throw GradleException("macOS is not supported as the required OpenGL version 4.3 is not supported.")
    else -> throw GradleException("Unsupported platform: ${operatingSystem.name}")
}

dependencies {
    "implementation"(platform(libs.lwjgl.bom))
    "runtimeOnly"("org.lwjgl:lwjgl::$lwjglNatives")
    "runtimeOnly"("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    "runtimeOnly"("org.lwjgl:lwjgl-opengl::$lwjglNatives")
}
