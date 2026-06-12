package de.hauschild.kompute.core.pipeline

import de.hauschild.kompute.core.data.ShaderData
import de.hauschild.kompute.core.shader.CompiledShader

/**
 * A single step in a [Pipeline], combining a compiled shader with its dispatch parameters.
 *
 * The [shader] is closed automatically after its dispatch completes. [data] may include
 * intermediate [de.hauschild.kompute.core.data.StorageBuffer]s shared with other stages —
 * their GPU handles are retained across stage boundaries without CPU round-trips.
 *
 * @property shader the compiled shader to dispatch
 * @property x number of work groups in the X dimension — must be ≥ 1
 * @property y number of work groups in the Y dimension — defaults to 1
 * @property z number of work groups in the Z dimension — defaults to 1
 * @property data shader inputs and outputs for this stage
 */
class Stage(
    val shader: CompiledShader,
    val x: Int,
    val y: Int = 1,
    val z: Int = 1,
    val data: List<ShaderData>
) : AutoCloseable by shader
