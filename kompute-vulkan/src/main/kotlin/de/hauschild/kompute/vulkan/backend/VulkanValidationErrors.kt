package de.hauschild.kompute.vulkan.backend

/**
 * Collects the errors reported by the Khronos validation layer, so that tests can fail on them.
 *
 * Only errors are recorded, warnings and informational messages are just logged. At most
 * [MAX_MESSAGES] messages are kept. Messages only arrive if the validation layer is enabled, see
 * [VulkanInstance].
 */
internal class VulkanValidationErrors {
    private val errors = ArrayList<String>()

    /**
     * Records the message of a validation error.
     *
     * @param message the message reported by the validation layer
     */
    @Synchronized
    fun record(message: String) {
        if (errors.size < MAX_MESSAGES) {
            errors.add(message)
        }
    }

    /**
     * Returns the number of validation errors recorded so far.
     *
     * @return the number of recorded errors
     */
    @Synchronized
    fun count(): Int = errors.size

    /**
     * Returns the validation errors recorded after the first [count] ones.
     *
     * @param count the number of errors to skip, usually an earlier [count]
     * @return the messages of the newer errors
     */
    @Synchronized
    fun since(count: Int): List<String> = errors.drop(count)

    companion object {
        private const val MAX_MESSAGES = 1_000

        /**
         * The errors reported by all Vulkan instances of this JVM.
         */
        val global = VulkanValidationErrors()
    }
}
