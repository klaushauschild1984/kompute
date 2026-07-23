package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.data.ShaderData
import de.hauschild.kompute.core.result.ShaderResult
import de.hauschild.kompute.core.shader.AbstractCompiledShader

class VulkanCompiledShader: AbstractCompiledShader() {
    override fun doDispatch(
        x: Int,
        y: Int,
        z: Int,
        data: List<ShaderData>
    ): ShaderResult {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}
