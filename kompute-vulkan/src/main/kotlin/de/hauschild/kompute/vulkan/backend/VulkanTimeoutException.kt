package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.exception.KomputeBackendException

/**
 * Thrown when the GPU did not finish a submission within the configured timeout, see the system
 * property `kompute.backend.vulkan.timeout`.
 *
 * The submission may still be running, so the resources it uses are not released and the backend
 * rejects further work. Close the backend after this exception.
 *
 * @param message
 * @param cause
 */
class VulkanTimeoutException(
    message: String,
    cause: Throwable? = null,
) : KomputeBackendException(message, cause)
