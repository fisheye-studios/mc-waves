#version 120

varying vec2 lmcoord;
varying vec2 texcoord;
varying vec4 glcolor;


uniform int worldTime;
uniform vec3 cameraPosition;


const int NUM_STEPS = 8;
const float PI        = 3.141592;
const float EPSILON    = 1e-3;
//#define AA

// sea
const int MAX_ITER_GEOMETRY = 5;
const int LOW_RES_GEOMETRY = 1;

const int FULL_RES_CHUNKS = 7;

const int ITER_FRAGMENT = 5;
const float SEA_HEIGHT = 0.6;
const float SEA_CHOPPY = 4.0;
const float SEA_SPEED = 5.0;
const float SEA_FREQ = 0.03;
const vec3 SEA_BASE = vec3(0.0, 0.09, 0.18);
const vec3 SEA_WATER_COLOR = vec3(0.8, 0.9, 0.6)*0.6;
//#define SEA_TIME (1.0 + worldTime * SEA_SPEED * 0.05)
const mat2 octave_m = mat2(1.6, 1.2, -1.2, 1.6);


float hash(vec2 p) {
    float h = dot(p, vec2(127.1, 311.7));
    return fract(sin(h) * 43758.5453123);
}
float noise(in vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return -1.0 + 2.0 * mix(mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}


float sea_octave(vec2 uv, float choppy) {
    uv += noise(uv);
    vec2 wv = 1.0 - abs(sin(uv));
    vec2 swv = abs(cos(uv));
    wv = mix(wv, swv, wv);
    return pow(1.0 - pow(wv.x * wv.y, 0.65), choppy);
}

float map(vec3 p, int iterations) {
    float SEA_TIME = (1.0 + worldTime * SEA_SPEED * 0.05);

    float freq = SEA_FREQ;
    float amp = SEA_HEIGHT;
    float choppy = SEA_CHOPPY;
    vec2 uv = p.xz;
    uv *= 3.0;
    uv.x *= 0.75;


    float d, h = 0.0;
    for (int i = 0; i < iterations; i++) {
        d = sea_octave((uv + SEA_TIME) * freq, choppy);
        d += sea_octave((uv - SEA_TIME) * freq, choppy);
        h += d * amp;
        uv *= octave_m;
        freq *= 1.9;
        amp *= 0.22;
        choppy = mix(choppy, 1.0, 0.2);
    }
    return p.y - h;
}


float distSquared(vec2 A, vec2 B) {
    return dot(A, B);
}

void main() {
    gl_Position = ftransform();

    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord  = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;




    vec3 finalPosition = gl_Vertex.xyz;
    float camDist = distSquared(finalPosition.xz, cameraPosition.xz);

    vec3 adjusted = cameraPosition.xyz + finalPosition.xyz;

    adjusted.y = 0;

    int iterations = MAX_ITER_GEOMETRY;
    if(camDist > FULL_RES_CHUNKS * 16)
        iterations = LOW_RES_GEOMETRY;

    float heightDiff = map(adjusted.xyz, iterations) * 5;
    finalPosition.y += SEA_HEIGHT - heightDiff;

    vec3 color = gl_Color.xyz;
    color -= SEA_WATER_COLOR * (SEA_HEIGHT + heightDiff) * .2;
    glcolor = vec4(color, 1);


    gl_Position = gl_ProjectionMatrix * gl_ModelViewMatrix * vec4(finalPosition, 1.0);
}