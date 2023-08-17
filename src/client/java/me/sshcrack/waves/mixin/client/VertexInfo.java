package me.sshcrack.waves.mixin.client;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import net.minecraft.util.math.MathHelper;

/**
 * Contains literally every imaginable info about the vertex and
 * has a function to lerp between them.
 * (Note: I've not used vectors to store variables here because they would allocate and free memory every frame)
 */
public record VertexInfo(
        double x, double y, double z,
        float red, float green, float blue,
        float u, float v,
        int light
) {

    public VertexInfo lerp(VertexInfo other, double delta) {
        float fDelta = (float) delta;

        var lX = MathHelper.lerp(delta, x, other.x);
        var lY = MathHelper.lerp(delta, y, other.y);
        var lZ = MathHelper.lerp(delta, z, other.z);


        var lR = MathHelper.lerp(fDelta, red, other.red);
        var lG = MathHelper.lerp(fDelta, green, other.green);
        var lB = MathHelper.lerp(fDelta, blue, other.blue);

        var lU = MathHelper.lerp(fDelta, u, other.u);
        var lV = MathHelper.lerp(fDelta, v, other.v);

        var lLight = MathHelper.lerp(fDelta, light, other.light);
        return new VertexInfo(
                lX, lY, lZ,
                lR, lG, lB,
                lU, lV,
                lLight
        );
    }
}
