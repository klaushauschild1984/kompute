#version 450 core
layout(local_size_x = 8, local_size_y = 8, local_size_z = 1) in;
layout(std430, binding = 0) readonly buffer MatrixA { float a[]; };
layout(std430, binding = 1) readonly buffer MatrixB { float b[]; };
layout(std430, binding = 2) writeonly buffer MatrixC { float c[]; };

void main() {
    ivec2 idx = ivec2(gl_GlobalInvocationID.xy);
    int n = int(sqrt(float(a.length())));
    float sum = 0.0;
    for (int k = 0; k < n; k++) {
        sum += a[idx.x * n + k] * b[k * n + idx.y];
    }
    c[idx.x * n + idx.y] = sum;
}