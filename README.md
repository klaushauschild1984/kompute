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
- Linux (Windows and macOS support planned)

## Usage

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

### Matrix multiplication

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
  * [ ] specific exception handling
  * [ ] generalize buffer setup
    * [x] API overhaul
    * [ ] UBO support
    * [ ] scalar uniform support
    * [ ] atomic counter support
    * [ ] image2D support
  * [ ] binding validation (collisions, shader inspection)
* [ ] OpenGL
  * [ ] optimization
    * [ ] shader caching
    * [ ] pre-compilation
    * [ ] multi-dispatch
* [ ] Vulkan
  * [ ] general implementation
* [ ] Showcasing
  * [ ] Mandelbrot-Set (plus visualization)
  * [ ] Monte-Carlo Pi calculation

## Contributing

Contributions are welcome. Please open an issue first to discuss what you would like to change.
