import org.gradle.internal.os.OperatingSystem

rootProject.name = "kompute"

include(
    "kompute-core",
    "kompute-vulkan",
    "kompute-coroutines",
    "kompute-serialization",
    "kompute-serialization:processor",
    "kompute-serialization:test"
)

// OpenGL requires 4.3, unsupported on macOS - excluded there so the whole build doesn't fail to
// configure just because these modules exist alongside kompute-vulkan.
if (!OperatingSystem.current().isMacOsX) {
    include(
        "kompute-opengl",
        "kompute-benchmark",
        "kompute-showcase",
    )
}
