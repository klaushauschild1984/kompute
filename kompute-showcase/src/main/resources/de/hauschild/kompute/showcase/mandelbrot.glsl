#version 430
layout (local_size_x =  $LOCAL_SIZE, local_size_y = $LOCAL_SIZE) in;
layout (binding = 0, rgba8) uniform writeonly image2D mandelbrot;
uniform int maxIterations;
uniform double centerX;
uniform double centerY;
uniform double zoom;

void main() {
    ivec2 pixel = ivec2(gl_GlobalInvocationID.xy);
    ivec2 size = imageSize(mandelbrot);
    if (pixel.x >= size.x || pixel.y >= size.y) return;
    double aspect = double(size.x) / double(size.y);
    double re = (((double(pixel.x) / double(size.x - 1)) * 2.0 - 1.0) * aspect) / zoom + centerX;
    double im = ((double(pixel.y) / double(size.y - 1)) * 2.0 - 1.0) / zoom + centerY;

    double zRe = 0.0, zIm = 0.0;
    int iter = 0;
    while (iter < maxIterations && zRe * zRe + zIm * zIm < 4.0) {
        double tmp = zRe * zRe - zIm * zIm + re;
        zIm = 2.0 * zRe * zIm + im;
        zRe = tmp;
        iter++;
    }

    if (iter == maxIterations) {
        imageStore(mandelbrot, pixel, vec4(0.0, 0.0, 0.0, 1.0));
    } else {
        float smoothIter = float(iter) - log2(log2(float(length(dvec2(zRe, zIm)))));
        float hue = fract(smoothIter * 0.01);
        float h = hue * 6.0;
        float i = floor(h);
        float f = h - i;
        float q = 1.0 - f;

        vec3 rgb;
        if (i < 1.0) rgb = vec3(1.0, f, 0.0);
        else if (i < 2.0) rgb = vec3(q, 1.0, 0.0);
        else if (i < 3.0) rgb = vec3(0.0, 1.0, f);
        else if (i < 4.0) rgb = vec3(0.0, q, 1.0);
        else if (i < 5.0) rgb = vec3(f, 0.0, 1.0);
        else rgb = vec3(1.0, 0.0, q);

        imageStore(mandelbrot, pixel, vec4(rgb, 1.0));
    }
}
