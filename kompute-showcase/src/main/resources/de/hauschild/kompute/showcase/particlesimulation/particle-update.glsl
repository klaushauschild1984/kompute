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

uniform int activeCount;
uniform float dt;
uniform float gravity;
uniform float maxAge;
uniform float boundsWidth;
uniform float boundsHeight;

void main() {
    uint i = gl_GlobalInvocationID.x;
    if (i >= uint(activeCount)) return;
    if (particles[i].age >= maxAge) return;

    particles[i].age += dt;
    particles[i].velocity.y += gravity * dt;
    particles[i].position   += particles[i].velocity * dt;

    if (particles[i].position.x < 0.0) {
        particles[i].position.x =  0.0;
        particles[i].velocity.x =  abs(particles[i].velocity.x);
    }
    if (particles[i].position.x >= boundsWidth) {
        particles[i].position.x =  boundsWidth - 1.0;
        particles[i].velocity.x = -abs(particles[i].velocity.x);
    }
    if (particles[i].position.y < 0.0) {
        particles[i].position.y =  0.0;
        particles[i].velocity.y =  abs(particles[i].velocity.y);
    }
    if (particles[i].position.y >= boundsHeight) {
        particles[i].position.y =  boundsHeight - 1.0;
        particles[i].velocity.y = -abs(particles[i].velocity.y);
    }
}
