# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [v0.6.0]

### Added
- `Image2D` — GPU-side image generation via `imageStore` — [docs](README.md#image2d)

## [v0.5.0] — 2026-06-07

### Added
- `AtomicCounter` — shared GPU counter for parallel accumulation — [docs](README.md#atomic-counter)
- `NamedUniform` — scalar, vector, and matrix uniforms — [docs](README.md#named-uniform)
- `StorageBuffer` read-write mode — [docs](README.md#storage-buffer)
- `kompute-showcase` module with Monte Carlo π approximation — [docs](README.md#showcase)

## [v0.4.0] — 2026-06-06

### Added
- `UniformBufferObject` — structured read-only shader parameters — [docs](README.md#uniform-buffer-object)

### Changed
- LWJGL dependency management via BOM

## [v0.3.0] — 2026-06-05

### Added
- Typed `StorageBuffer<T>` — `FloatArray`, `IntArray`, `DoubleArray`, `ByteArray` — [docs](README.md#storage-buffer)
- Diktat code style — replaces KtLint

### Changed
- Cross-validation for duplicate binding indices

## [v0.2.0] — 2026-06-04

### Added
- Typed exception hierarchy — `KomputeConfigurationException`, `KomputeBackendException`
- Binding validation — index bounds checked against GPU limits
- Detekt static analysis

### Changed
- Automated GitHub Actions release workflow

## [v0.1.0] — 2026-06-03

### Added
- OpenGL compute shader backend via LWJGL — [docs](README.md#getting-started)
- `StorageBuffer` — CPU↔GPU data exchange — [docs](README.md#storage-buffer)
- JMH benchmarks — `kompute-benchmark` module — [docs](README.md#performance)

[v0.6.0]: https://github.com/klaushauschild1984/kompute/compare/v0.5.0...v0.6.0
[v0.5.0]: https://github.com/klaushauschild1984/kompute/compare/v0.4.0...v0.5.0
[v0.4.0]: https://github.com/klaushauschild1984/kompute/compare/v0.3.0...v0.4.0
[v0.3.0]: https://github.com/klaushauschild1984/kompute/compare/v0.2.0...v0.3.0
[v0.2.0]: https://github.com/klaushauschild1984/kompute/compare/v0.1.0...v0.2.0
[v0.1.0]: https://github.com/klaushauschild1984/kompute/releases/tag/v0.1.0
