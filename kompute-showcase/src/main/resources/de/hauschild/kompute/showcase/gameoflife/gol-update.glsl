#version 430
layout(local_size_x = $LOCAL_SIZE) in;

layout(std430, binding = 0) buffer CellBuffer {
    int cells[];
};

uniform int width;
uniform int height;
uniform int readOffset;
uniform int writeOffset;

void main() {
    uint idx = gl_GlobalInvocationID.x;
    if (idx >= uint(width * height)) return;

    int x = int(idx) % width;
    int y = int(idx) / width;

    int live = 0;
    for (int dy = -1; dy <= 1; dy++) {
        for (int dx = -1; dx <= 1; dx++) {
            if (dx == 0 && dy == 0) continue;
            int nx = (x + dx + width) % width;
            int ny = (y + dy + height) % height;
            if (cells[readOffset + ny * width + nx] > 0) live++;
        }
    }

    int age = cells[readOffset + y * width + x];
    int next = 0;
    if (age > 0 && (live == 2 || live == 3)) next = age + 1;
    else if (age == 0 && live == 3) next = 1;

    cells[writeOffset + y * width + x] = next;
}
