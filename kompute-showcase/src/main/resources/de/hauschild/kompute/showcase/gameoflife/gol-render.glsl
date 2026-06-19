#version 430
layout(local_size_x = $LOCAL_SIZE) in;

layout(std430, binding = 0) readonly buffer CellBuffer {
    int cells[];
};

layout(rgba8, binding = 0) uniform writeonly image2D canvas;

uniform int width;
uniform int height;
uniform int displayOffset;

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
    uint idx = gl_GlobalInvocationID.x;
    if (idx >= uint(width * height)) return;

    int x = int(idx) % width;
    int y = int(idx) / width;

    int age = cells[displayOffset + y * width + x];

    vec4 color;
    if (age == 0) {
        color = vec4(0.05, 0.05, 0.1, 1.0);
    } else {
        color = vec4(hue(float(age % 128) / 128.0), 1.0);
    }

    imageStore(canvas, ivec2(x, y), color);
}
