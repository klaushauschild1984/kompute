plugins {
    id("kompute.kotlin-conventions")
    id("kompute.vulkan-conventions")
    `java-test-fixtures`
}

dependencies {
    api(project(":kompute-core"))
    implementation(libs.lwjgl)
    implementation(libs.lwjgl.vulkan)
    implementation(libs.lwjgl.shaderc)
    implementation(libs.lwjgl.spvc)
    implementation(libs.lwjgl.vma)

    testFixturesImplementation(platform(libs.junit.bom))
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api")
}

tasks.withType<Test>().configureEach {
    // -PvulkanValidation=true runs the tests with the Khronos validation layer and fails them on its errors
    providers.gradleProperty("vulkanValidation").orNull?.let {
        systemProperty("kompute.backend.vulkan.validation", it)
    }
}

// Runs the tests a second time through staging buffers only. Devices with unified memory (e.g. integrated GPUs)
// use directly accessible memory by default, so this covers the path of all other devices on such machines.
val testWithStaging = tasks.register<Test>("testWithStaging") {
    group = "verification"
    description = "Runs the tests with direct memory access disabled, i.e. through staging buffers"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform()
    systemProperty("kompute.backend.vulkan.directAccess", "false")
    testLogging {
        showStandardStreams = true
    }
}

tasks.check {
    dependsOn(testWithStaging)
}
