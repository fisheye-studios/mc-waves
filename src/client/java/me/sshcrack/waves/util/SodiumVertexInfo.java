package me.sshcrack.waves.util;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import net.minecraft.util.math.MathHelper;

public record SodiumVertexInfo(float x, float y, float z, float u, float v) {
    public static SodiumVertexInfo of(ModelQuadView quad, int i) {
        return new SodiumVertexInfo(
                quad.getX(i), quad.getY(i), quad.getZ(i),
                quad.getTexU(i), quad.getTexV(i)
        );
    }

    public SodiumVertexInfo lerp(SodiumVertexInfo other, float delta) {
        return new SodiumVertexInfo(
                MathHelper.lerp(delta, x, other.x),
                MathHelper.lerp(delta, y, other.y),
                MathHelper.lerp(delta, z, other.z),

                MathHelper.lerp(delta, u, other.u),
                MathHelper.lerp(delta, v, other.v)
        );
    }
}
