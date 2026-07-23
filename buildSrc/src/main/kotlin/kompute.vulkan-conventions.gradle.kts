import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.internal.os.OperatingSystem

val libs = the<LibrariesForLibs>()

val operatingSystem: OperatingSystem = OperatingSystem.current()
val lwjglNatives = when {
    operatingSystem.isWindows -> "natives-windows"
    operatingSystem.isLinux   -> "natives-linux"
    operatingSystem.isMacOsX  ->
        if (System.getProperty("os.arch").startsWith("aarch64")) "natives-macos-arm64" else "natives-macos"
    else -> throw GradleException("Unsupported platform: ${operatingSystem.name}")
}

dependencies {
    "implementation"(platform(libs.lwjgl.bom))
    "runtimeOnly"("org.lwjgl:lwjgl::$lwjglNatives")
    // lwjgl-vulkan only ships natives for macOS (MoltenVK loader); on Windows/Linux the
    // Vulkan loader is provided by the GPU driver / Vulkan SDK already present on the system.
    if (operatingSystem.isMacOsX) {
        "runtimeOnly"("org.lwjgl:lwjgl-vulkan::$lwjglNatives")
    }
    "runtimeOnly"("org.lwjgl:lwjgl-shaderc::$lwjglNatives")
    "runtimeOnly"("org.lwjgl:lwjgl-vma::$lwjglNatives")
}
