#version 430 core
layout (local_size_x = 1) in;

layout (std430, binding = 0) readonly buffer InputBuffer {
    float values[];
} source;

layout (std430, binding = 1) writeonly buffer OutputBuffer {
    float values[];
} result;

void main() {
    result.values[gl_GlobalInvocationID.x] = source.values[gl_GlobalInvocationID.x];
}
