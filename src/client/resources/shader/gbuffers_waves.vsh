// Note: Things beggining with a comment are just straight up removed (when compiling) keep that in mind!

//#version 120

// replace later with waves_time and waves_cameraPosition to not have duplicates
uniform int waves_time;
uniform vec3 waves_cameraPosition;


const int waves_NUM_STEPS = 8;
const float waves_PI        = 3.141592;
const float waves_EPSILON    = 1e-3;
//#define AA

// sea
const int waves_MAX_ITER_GEOMETRY = 5;
const int waves_LOW_RES_GEOMETRY = 1;

const int waves_FULL_RES_CHUNKS = 7;

const int waves_ITER_FRAGMENT = 5;
const float waves_SEA_HEIGHT = 0.6;
const float waves_SEA_CHOPPY = 4.0;
const float waves_SEA_SPEED = 5.0;
const float waves_SEA_FREQ = 0.03;
const vec3 waves_SEA_BASE = vec3(0.0, 0.09, 0.18);
const vec3 waves_SEA_WATER_COLOR = vec3(0.8, 0.9, 0.6)*0.6;
//#define SEA_TIME (1.0 + worldTime * SEA_SPEED * 0.05)
const mat2 waves_octave_m = mat2(1.6, 1.2, -1.2, 1.6);




// REQUIRED
vec4 waves_vertex;
vec4 waves_position;
vec4 waves_normal;


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
    float SEA_TIME = (1.0 + waves_time * waves_SEA_SPEED * 0.05);

    float freq = waves_SEA_FREQ;
    float amp = waves_SEA_HEIGHT;
    float choppy = waves_SEA_CHOPPY;
    vec2 uv = p.xz;
    uv *= 3.0;
    uv.x *= 0.75;


    float d, h = 0.0;
    for (int i = 0; i < iterations; i++) {
        d = sea_octave((uv + SEA_TIME) * freq, choppy);
        d += sea_octave((uv - SEA_TIME) * freq, choppy);
        h += d * amp;
        uv *= waves_octave_m;
        freq *= 1.9;
        amp *= 0.22;
        choppy = mix(choppy, 1.0, 0.2);
    }
    return p.y - h;
}


float distSquared(vec2 A, vec2 B) {
    return dot(A, B);
}

// tracing
vec3 waves_getNormal(vec3 p, float eps, int iterations) {
    vec3 n;
    n.y = map(p, iterations);
    n.x = map(vec3(p.x+eps,p.y,p.z), iterations) - n.y;
    n.z = map(vec3(p.x,p.y,p.z+eps), iterations) - n.y;
    n.y = eps;
    return normalize(n);
}


void compute(vec4 vertex) {
    float camDistSqrd = distSquared(vertex.xz, waves_cameraPosition.xz);

    vec3 adjusted = waves_cameraPosition.xyz + vertex.xyz;

    adjusted.y = 0;

    int iterations = waves_MAX_ITER_GEOMETRY;
    if(camDistSqrd > waves_FULL_RES_CHUNKS * 16)
    iterations = waves_LOW_RES_GEOMETRY;

    float heightDiff = map(adjusted.xyz, iterations) * 5;
    vertex.y += waves_SEA_HEIGHT - heightDiff;

    //vec3 color = gl_Color.xyz;
    //color -= SEA_WATER_COLOR * (SEA_HEIGHT + heightDiff) * .2;
    //glcolor = vec4(color, 1);


    waves_position = gl_ProjectionMatrix * gl_ModelViewMatrix * vertex;
    waves_vertex = vertex;

    vec3 camDist = waves_vertex.xyz - waves_cameraPosition;
    float RESOLUTION = .05;
    waves_normal = vec4(
        waves_getNormal(vertex.xyz, dot(camDist,camDist) * RESOLUTION, waves_ITER_FRAGMENT),

        1
    );
}
