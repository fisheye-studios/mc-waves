package me.sshcrack.waves;

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
                MathHelper.lerp(x, other.x, delta),
                MathHelper.lerp(y, other.y, delta),
                MathHelper.lerp(z, other.z, delta),

                MathHelper.lerp(u, other.u, delta),
                MathHelper.lerp(v, other.v, delta)
        );
    }
}
