package de.hauschild.kompute.backend.kotlin

interface BufferScope {
    fun getFloat(name: String): FloatArray

    fun putFloat(
        name: String,
        data: FloatArray,
    )
}
