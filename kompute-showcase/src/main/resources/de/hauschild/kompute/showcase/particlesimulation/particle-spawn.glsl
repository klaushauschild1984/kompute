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
uniform int spawnCount;
uniform float spawnX;
uniform float spawnY;
uniform int seed;

uint xorshift(uint x) {
    x ^= x << 13u;
    x ^= x >> 17u;
    x ^= x << 5u;
    return x;
}

float rand(uint s) {
    return float(xorshift(s)) / 4294967295.0;
}

vec3 hue(float h) {
    float s = h * 6.0;
    float f = fract(s);
    float q = 1.0 - f;
    int i = int(s) % 6;
    if (i == 0) return vec3(1.0, f,   0.0);
    if (i == 1) return vec3(q,   1.0, 0.0);
    if (i == 2) return vec3(0.0, 1.0, f  );
    if (i == 3) return vec3(0.0, q,   1.0);
    if (i == 4) return vec3(f,   0.0, 1.0);
               return vec3(1.0, 0.0, q  );
}

void main() {
    uint i = gl_GlobalInvocationID.x;
    if (i >= uint(spawnCount)) return;

    uint s0 = xorshift(uint(seed) ^ xorshift(uint(activeCount) + i * 7919u));
    uint s1 = xorshift(s0);
    uint s2 = xorshift(s1);

    float speed  = rand(s0) * 250.0 + 50.0;
//    float speed  = rand(s0) * 20.0 + 10.0;
    float spread = (rand(s1) - 0.5) * 3.14159;
    float vx = sin(spread) * speed;
    float vy = -cos(spread) * speed;

    particles[uint(activeCount) + i].position = vec2(spawnX, spawnY);
    particles[uint(activeCount) + i].velocity = vec2(vx, vy);
    particles[uint(activeCount) + i].color    = hue(rand(s2 + 2u));
    particles[uint(activeCount) + i].age      = 0.0;
}
