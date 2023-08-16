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

        var lX = MathHelper.lerp(x, other.x, delta);
        var lY = MathHelper.lerp(y, other.y, delta);
        var lZ = MathHelper.lerp(z, other.z, delta);


        var lR = MathHelper.lerp(red, other.red, fDelta);
        var lG = MathHelper.lerp(green, other.green, fDelta);
        var lB = MathHelper.lerp(blue, other.blue, fDelta);

        var lU = MathHelper.lerp(u, other.u, fDelta);
        var lV = MathHelper.lerp(v, other.v, fDelta);

        // dear mojang devlopers: Why is this the other way around??
        var lLight = MathHelper.lerp(fDelta, light, other.light);
        return new VertexInfo(
                lX, lY, lZ,
                lR, lG, lB,
                lU, lV,
                lLight
        );
    }
}
