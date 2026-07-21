package de.hauschild.kompute.types

import de.hauschild.kompute.serialization.annotation.Align
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * ```glsl
 * struct PointLight {
 *     vec3 position;
 *     vec3 color;
 *     float intensity;
 *     float constantAttenuation;
 *     float linearAttenuation;
 *     float quadraticAttenuation;
 * };
 *
 * layout(std140, binding = 0) uniform Lighting {
 *     PointLight light;
 * };
 * ```
 *
 * The GLSL struct's member order must match the declaration order above — std140 packs members
 * positionally, so reordering them on the shader side would no longer match the generated layout.
 *
 * @property position the light's position
 * @property color the light's color
 * @property intensity the light's intensity
 * @property constantAttenuation the constant term of the attenuation formula
 * @property linearAttenuation the linear term of the attenuation formula
 * @property quadraticAttenuation the quadratic term of the attenuation formula
 */
@GpuStruct
@Align(16)
data class PointLight(
    @GpuField val position: Vector3f,
    @GpuField val color: Vector3f,
    @GpuField val intensity: Float,
    @GpuField val constantAttenuation: Float,
    @GpuField val linearAttenuation: Float,
    @GpuField val quadraticAttenuation: Float,
)
