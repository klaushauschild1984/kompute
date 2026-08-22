package de.hauschild.kompute.types

import de.hauschild.kompute.serialization.annotation.Align
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * ```glsl
 * struct SpotLight {
 *     vec3 position;
 *     vec3 direction;
 *     vec3 color;
 *     float intensity;
 *     float constantAttenuation;
 *     float linearAttenuation;
 *     float quadraticAttenuation;
 *     float innerCutOff;
 *     float outerCutOff;
 * };
 *
 * layout(std140, binding = 0) uniform Lighting {
 *     SpotLight light;
 * };
 * ```
 *
 * The GLSL struct's member order must match the declaration order above — std140 packs members
 * positionally, so reordering them on the shader side would no longer match the generated layout.
 *
 * @property position the light's position
 * @property direction the direction the light is shining, e.g. a normalized vector
 * @property color the light's color
 * @property intensity the light's intensity
 * @property constantAttenuation the constant term of the attenuation formula
 * @property linearAttenuation the linear term of the attenuation formula
 * @property quadraticAttenuation the quadratic term of the attenuation formula
 * @property innerCutOff the cosine of the inner cone angle, beyond which the light starts to fade out
 * @property outerCutOff the cosine of the outer cone angle, beyond which the light contributes nothing
 */
@GpuStruct
@Align(16)
data class SpotLight(
    @GpuField val position: Vector3f,
    @GpuField val direction: Vector3f,
    @GpuField val color: Vector3f,
    @GpuField val intensity: Float,
    @GpuField val constantAttenuation: Float,
    @GpuField val linearAttenuation: Float,
    @GpuField val quadraticAttenuation: Float,
    @GpuField val innerCutOff: Float,
    @GpuField val outerCutOff: Float,
)
