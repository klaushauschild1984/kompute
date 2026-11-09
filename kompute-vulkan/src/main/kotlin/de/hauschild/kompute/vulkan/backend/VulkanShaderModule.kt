/**
 * Bundles the [VulkanShaderModule] and the [VulkanShaderBinding] reflected from its SPIR-V bytecode.
 */

package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.exception.KomputeShaderSourceException
import de.hauschild.kompute.core.exception.requireBackendInitialization
import de.hauschild.kompute.core.shader.ShaderSource
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.shaderc.Shaderc.shaderc_compilation_status_success
import org.lwjgl.util.shaderc.Shaderc.shaderc_compile_into_spv
import org.lwjgl.util.shaderc.Shaderc.shaderc_compiler_initialize
import org.lwjgl.util.shaderc.Shaderc.shaderc_compiler_release
import org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_compute_shader
import org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_bytes
import org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_compilation_status
import org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_error_message
import org.lwjgl.util.shaderc.Shaderc.shaderc_result_release
import org.lwjgl.util.spvc.Spv
import org.lwjgl.util.spvc.Spvc
import org.lwjgl.util.spvc.SpvcReflectedResource
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkShaderModuleCreateInfo

/**
 * A single descriptor binding reflected from a shader's SPIR-V bytecode via SPIRV-Cross.
 *
 * @property descriptorSet the `set` decoration.
 * @property binding the `binding` decoration.
 * @property descriptorType the raw `VkDescriptorType` value this resource maps to.
 */
data class VulkanShaderBinding(
    val descriptorSet: Int,
    val binding: Int,
    val descriptorType: Int,
)

/**
 * A Vulkan shader module handling compilation and pipeline setup.
 *
 * The descriptor bindings a shader declares (`layout(set = ..., binding = ...)`) are reflected
 * from its SPIR-V bytecode via SPIRV-Cross - the shader itself is the source of truth for its
 * binding layout, so `ShaderData` supplied at dispatch time only needs to be validated against
 * [bindings], not used to derive it.
 *
 * @param source the [ShaderSource]
 * @param device the [VulkanDevice]
 */
@Suppress("TooGenericExceptionCaught") // cleans up on any failure and rethrows
class VulkanShaderModule(source: ShaderSource, private val device: VulkanDevice) : AutoCloseable {
    private val compiler = shaderc_compiler_initialize()
    private var result: Long = -1
    private var shaderModule: Long = VK10.VK_NULL_HANDLE

    /**
     * The descriptor bindings reflected from the shader's SPIR-V bytecode.
     */
    val bindings: List<VulkanShaderBinding>

    init {
        try {
            val shaderBytecode: ByteArray = when (source) {
                is ShaderSource.Glsl -> compile(source.resolve())
                is ShaderSource.Spirv -> source.resolve().also { bytes ->
                    if (bytes.size % Int.SIZE_BYTES != 0) {
                        throw KomputeShaderSourceException(
                            "SPIR-V bytecode size must be a multiple of ${Int.SIZE_BYTES} bytes, was ${bytes.size}",
                        )
                    }
                }
            }
            bindings = reflectBindings(shaderBytecode)
            shaderModule = createShaderModule(shaderBytecode)
        } catch (t: Throwable) {
            close()
            throw t
        }
    }

    /**
     * Returns the raw `VkShaderModule` handle.
     *
     * @return the `VkShaderModule` handle (non-dispatchable, hence a plain [Long])
     */
    fun handle(): Long = shaderModule

    private fun reflectBindings(shaderBytecode: ByteArray): List<VulkanShaderBinding> =
        MemoryStack.stackPush().use { stack ->
            val contextPointer = stack.mallocPointer(1)
            var spvcResult = Spvc.spvc_context_create(contextPointer)
            requireBackendInitialization(spvcResult == Spvc.SPVC_SUCCESS) {
                "Failed to create SPIRV-Cross context: $spvcResult"
            }
            val context = contextPointer.get(0)
            try {
                val spirvWords = stack.malloc(shaderBytecode.size)
                    .put(shaderBytecode)
                    .flip()
                    .asIntBuffer()
                val parsedIrPointer = stack.mallocPointer(1)
                spvcResult = Spvc.spvc_context_parse_spirv(
                    context, spirvWords, spirvWords.remaining().toLong(), parsedIrPointer,
                )
                requireBackendInitialization(spvcResult == Spvc.SPVC_SUCCESS) {
                    "Failed to parse SPIR-V for reflection: ${Spvc.spvc_context_get_last_error_string(context)}"
                }

                val compilerPointer = stack.mallocPointer(1)
                spvcResult = Spvc.spvc_context_create_compiler(
                    context,
                    Spvc.SPVC_BACKEND_NONE,
                    parsedIrPointer.get(0),
                    Spvc.SPVC_CAPTURE_MODE_TAKE_OWNERSHIP,
                    compilerPointer,
                )
                requireBackendInitialization(spvcResult == Spvc.SPVC_SUCCESS) {
                    "Failed to create SPIRV-Cross compiler: ${Spvc.spvc_context_get_last_error_string(context)}"
                }
                val reflectionCompiler = compilerPointer.get(0)

                val resourcesPointer = stack.mallocPointer(1)
                spvcResult = Spvc.spvc_compiler_create_shader_resources(reflectionCompiler, resourcesPointer)
                requireBackendInitialization(spvcResult == Spvc.SPVC_SUCCESS) {
                    "Failed to reflect shader resources: ${Spvc.spvc_context_get_last_error_string(context)}"
                }
                val resources = resourcesPointer.get(0)

                RESOURCE_TYPE_TO_DESCRIPTOR_TYPE.flatMap { (resourceType, descriptorType) ->
                    reflectBindingsForResourceType(reflectionCompiler, resources, resourceType, descriptorType, stack)
                }
            } finally {
                Spvc.spvc_context_destroy(context)
            }
        }

    private fun reflectBindingsForResourceType(
        reflectionCompiler: Long,
        resources: Long,
        resourceType: Int,
        descriptorType: Int,
        stack: MemoryStack,
    ): List<VulkanShaderBinding> {
        val resourceListPointer = stack.mallocPointer(1)
        val resourceCountPointer = stack.mallocPointer(1)
        val spvcResult = Spvc.spvc_resources_get_resource_list_for_type(
            resources, resourceType, resourceListPointer, resourceCountPointer,
        )
        requireBackendInitialization(spvcResult == Spvc.SPVC_SUCCESS) {
            "Failed to list shader resources of type $resourceType"
        }
        val resourceCount = resourceCountPointer.get(0).toInt()
        if (resourceCount == 0) {
            return emptyList()
        }

        return SpvcReflectedResource.create(resourceListPointer.get(0), resourceCount).map { resource ->
            VulkanShaderBinding(
                descriptorSet = Spvc.spvc_compiler_get_decoration(
                    reflectionCompiler, resource.id(), Spv.SpvDecorationDescriptorSet,
                ),
                binding = Spvc.spvc_compiler_get_decoration(
                    reflectionCompiler, resource.id(), Spv.SpvDecorationBinding,
                ),
                descriptorType = descriptorType,
            )
        }
    }

    private fun createShaderModule(shaderBytecode: ByteArray): Long =
        MemoryStack.stackPush().use { stack ->
            val code = stack.malloc(shaderBytecode.size).put(shaderBytecode).flip()
            val createInfo = VkShaderModuleCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                .pCode(code)
            val pointer = stack.mallocLong(1)
            val vkResult = VK10.vkCreateShaderModule(device.device, createInfo, null, pointer)
            requireBackendInitialization(vkResult == VK10.VK_SUCCESS) {
                "Failed to create Vulkan shader module: ${vkResultName(vkResult)}"
            }
            pointer.get(0)
        }

    private fun compile(glsl: String): ByteArray {
        result = shaderc_compile_into_spv(
            compiler, glsl, shaderc_glsl_compute_shader, "shader", "main", 0
        )
        if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
            val errorMessage = shaderc_result_get_error_message(result)
            throw KomputeShaderSourceException("Failed to compile shader: $errorMessage")
        }
        val byteBuffer = checkNotNull(shaderc_result_get_bytes(result)) { "shaderc returned no bytecode" }
        return ByteArray(byteBuffer.remaining()).also { byteBuffer.get(it) }
    }

    override fun close() {
        if (shaderModule != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyShaderModule(device.device, shaderModule, null)
        }
        if (result != -1L) {
            shaderc_result_release(result)
        }
        shaderc_compiler_release(compiler)
    }

    companion object {
        private val RESOURCE_TYPE_TO_DESCRIPTOR_TYPE = listOf(
            Spvc.SPVC_RESOURCE_TYPE_UNIFORM_BUFFER to VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
            Spvc.SPVC_RESOURCE_TYPE_STORAGE_BUFFER to VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
            Spvc.SPVC_RESOURCE_TYPE_STORAGE_IMAGE to VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
        )
    }
}
