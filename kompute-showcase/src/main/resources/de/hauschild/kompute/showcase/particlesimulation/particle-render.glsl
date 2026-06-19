#version 430
layout(local_size_x = $LOCAL_SIZE) in;

struct Particle {
    vec2 position;
    vec2 velocity;
    vec3 color;
    float age;
};

layout(std430, binding = 0) buffer ParticleBuffer {
    Particle particles[];
};

layout(binding = 0, rgba8) uniform writeonly image2D canvas;

uniform int activeCount;
uniform float maxAge;

void main() {
    uint i = gl_GlobalInvocationID.x;
    if (i >= uint(activeCount)) return;
    if (particles[i].age >= maxAge) return;

    float fade  = 1.0 - particles[i].age / maxAge;
    ivec2 pixel = ivec2(particles[i].position);
    ivec2 size  = imageSize(canvas);
    if (pixel.x < 0 || pixel.x >= size.x || pixel.y < 0 || pixel.y >= size.y) return;

    imageStore(canvas, pixel, vec4(particles[i].color * fade, 1.0));
}
