#version 430
layout (local_size_x = $LOCAL_SIZE) in;
layout (binding = 0) uniform atomic_uint hits;

uint pcg(uint v) {
    uint state = v * 747796405u + 2891336453u;
    uint word = ((state >> ((state >> 28u) + 4u)) ^ state) * 277803737u;
    return (word >> 22u) ^ word;
}

void main() {
    uint id = gl_GlobalInvocationID.x;
    float x = float(pcg(id)) / float(0xFFFFFFFFu);
    float y = float(pcg(id + 0x9e3779b9u)) / float(0xFFFFFFFFu);
    if (x * x + y * y <= 1.0) {
        atomicCounterIncrement(hits);
    }
}
