# Kompute: GPU Compute Shaders for Kotlin

Kompute is a Kotlin library designed to simplify the integration of GPU compute shaders into Kotlin applications. It
provides a high-level API for managing GPU resources, executing compute operations, and handling data transfers between
the CPU and GPU. With Kompute, developers can leverage the power of GPU acceleration for computationally intensive
tasks, such as machine learning inference, physics simulations, and data processing.

[![CI](https://github.com/klaushauschild1984/kompute/actions/workflows/ci.yml/badge.svg)](https://github.com/klaushauschild1984/kompute/actions/workflows/ci.yml)

![Coverage](.github/badges/jacoco.svg)

## Requirements

- JDK 21+
- OpenGL 4.3+ capable GPU
- Linux or Windows

> **macOS:** OpenGL support on macOS is limited to 4.1 — compute shaders require 4.3 and are therefore not supported.
> macOS support depends on the upcoming Vulkan backend.

## Getting Started

Add the JitPack repository and the dependency to your build.

> **Note:** LWJGL native bindings are not included transitively — add the ones matching your target platform.

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.klaushauschild1984.kompute:kompute-opengl:v0.2.0")

    runtimeOnly("org.lwjgl:lwjgl:3.3.4:natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-glfw:3.3.4:natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-opengl:3.3.4:natives-linux")
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.klaushauschild1984.kompute</groupId>
        <artifactId>kompute-opengl</artifactId>
        <version>v0.2.0</version>
    </dependency>
    <dependency>
        <groupId>org.lwjgl</groupId>
        <artifactId>lwjgl</artifactId>
        <version>3.3.4</version>
        <classifier>natives-linux</classifier>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.lwjgl</groupId>
        <artifactId>lwjgl-glfw</artifactId>
        <version>3.3.4</version>
        <classifier>natives-linux</classifier>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.lwjgl</groupId>
        <artifactId>lwjgl-opengl</artifactId>
        <version>3.3.4</version>
        <classifier>natives-linux</classifier>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

## Usage

Select a backend, attach a compute shader, configure storage buffers, dispatch and read results.

### Kotlin

```kotlin
Kompute.openGL().use { openGL ->
    val result = openGL
        .shader(ShaderSource.Code(glslCode))
        .data(
            StorageBuffer(0).data(input),
            StorageBuffer(1).size(128).asOutput("result"),
        )
        .dispatch(x = 64)
        .execute()
    println(result.storageBuffer("result").contentToString())
}
```

### Java

```java
try (Backend backend = Kompute.openGL()) {
    float[] result = backend
        .shader(new ShaderSource.Code(glslCode))
        .data(
            new ShaderData.StorageBuffer(0).data(input),
            new ShaderData.StorageBuffer(1).size(128).asOutput("result")
        )
        .dispatch(64)
        .execute()
        .storageBuffer("result");
}
```

## Shader Sources

Shaders can be loaded from different sources:

```kotlin
// Inline GLSL
ShaderSource.Code("...")

// File on disk
ShaderSource.File(Path.of("shaders/multiply.glsl"))

// Classpath resource
ShaderSource.Stream(MyClass::class.java.getResourceAsStream("shader.glsl")!!)
```

## Storage Buffers

```kotlin
// Input: provide data to the shader
StorageBuffer(0).data(floatArrayOf(1f, 2f, 3f))

// Output: shader writes results here
StorageBuffer(1).size(128).asOutput("result")

// Read-write: initialized with data, result readable afterwards
StorageBuffer(2).data(existing).asOutput("updated")
```

## Performance

Benchmarks are implemented using [JMH](https://github.com/openjdk/jmh) in the `kompute-benchmark` module.
Each benchmark compares a naive Kotlin CPU implementation against the OpenGL compute shader backend.
Backend initialization and shader compilation are excluded from the measurement — only buffer transfer,
dispatch and readback are measured.

### Matrix multiplication

Matrix multiplication computes `C = A × B` for two square float matrices.
The Kotlin implementation uses a naive O(n³) triple loop. The OpenGL shader launches one thread per
output element in a 2D workgroup grid (`local_size_x = 8, local_size_y = 8`).

| Size of matrix | Kotlin (ms) | OpenGL (ms) | Speedup |
|----------------|-------------|-------------|---------|
| 128×128        | 1,404       | 0,208       | ~6,7×   |
| 512×512        | 124,424     | 2,172       | ~57×    |
| 1024×1024      | 2735,201    | 27,989      | ~97×    |

```mermaid
xychart-beta
  title "OpenGL Speedup over Kotlin CPU"
  x-axis ["128×128", "512×512", "1024×1024"]
  y-axis "Speedup (×)" 0 --> 100
  bar [6.7, 57.3, 97.7]
```

## Building

```bash
./gradlew build
```

Tests require a display server and OpenGL-capable GPU. On headless systems use:

```bash
xvfb-run ./gradlew build
```

## TODOs

A collection of topics I want to address in the future enhancing the library.

* [ ] API
  * [x] specific exception handling
  * [ ] generalize buffer setup
    * [x] API overhaul
    * [ ] UBO support
    * [ ] scalar uniform support
    * [ ] atomic counter support
    * [ ] image2D support
  * [ ] binding validation
    * [x] index collisions
    * [x] name collisions
    * [ ] basic binding index verification against shader source
* [ ] OpenGL
  * [x] backend-specific binding validation
  * [ ] optimization
    * [ ] shader caching
    * [ ] pre-compilation
    * [ ] multi-dispatch
* [ ] Vulkan
  * [ ] general implementation
* [ ] Showcasing
  * [ ] Mandelbrot-Set (plus visualization)
  * [ ] Monte-Carlo Pi calculation

## Milestones

| Version                                                                       | Focus |
|-------------------------------------------------------------------------------|-------|
| [`v0.1.0`](https://github.com/klaushauschild1984/kompute/releases/tag/v0.1.0) | OpenGL Storage Buffer — initial release |
| `v0.2.0`                                                                      | Stability (exception handling, binding validation) |
| `v0.3.0`                                                                      | UBO + scalar uniform support |
| `v0.4.0`                                                                      | Windows support |
| `v0.5.0`                                                                      | `image2D` support |
| `v0.6.0`                                                                      | OpenGL optimizations (shader caching, pre-compilation, multi-dispatch) |
| `v0.7.0`                                                                      | Vulkan backend |
| `v1.0.0`                                                                      | Stable, complete API |

## Contributing

Contributions are welcome. Please open an issue first to discuss what you would like to change.
