package de.hauschild.kompute.types

import de.hauschild.kompute.serialization.annotation.Align
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * ```glsl
 * struct DirectionalLight {
 *     vec3 direction;
 *     vec3 color;
 *     float intensity;
 * };
 *
 * layout(std140, binding = 0) uniform Lighting {
 *     DirectionalLight light;
 * };
 * ```
 *
 * The GLSL struct's member order must match the declaration order above — std140 packs members
 * positionally, so reordering them on the shader side would no longer match the generated layout.
 *
 * @property direction the direction the light is shining, e.g. a normalized vector
 * @property color the light's color
 * @property intensity the light's intensity
 */
@GpuStruct
@Align(16)
data class DirectionalLight(
    @GpuField val direction: Vector3f,
    @GpuField val color: Vector3f,
    @GpuField val intensity: Float,
)
