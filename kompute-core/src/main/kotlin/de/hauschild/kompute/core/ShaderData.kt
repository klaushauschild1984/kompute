package de.hauschild.kompute.core

sealed interface ShaderData {
    fun validate(): Unit

    class StorageBuffer(
        val index: Int,
    ) : ShaderData {
        var data: FloatArray? = null
        var size: Int? = null
        var outputName: String? = null

        fun data(data: FloatArray): StorageBuffer {
            this.data = data
            return this
        }

        fun size(size: Int): StorageBuffer {
            this.size = size
            return this
        }

        fun asOutput(name: String): StorageBuffer {
            this.outputName = name
            return this
        }

        override fun validate() {
            require(data != null || size != null) { "Either data or size must be provided for StorageBuffer" }
            if (data != null) {
                require(size == null) { "Size should not be combined together with data" }
            }
            if (size != null) {
                require(outputName != null) { "Output name must be provided for StorageBuffer with size" }
            }
        }
    }
}
