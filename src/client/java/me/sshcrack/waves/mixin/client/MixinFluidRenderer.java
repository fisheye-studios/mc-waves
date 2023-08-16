package me.sshcrack.waves.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.sshcrack.waves.Debug;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.FluidRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

@Mixin(value = FluidRenderer.class, priority = 5000)
public class MixinFluidRenderer {
    /**
     * Variables go like this:
     * 1,1
     * {@link #startVertZ} ---------- {@link #endVertZ}
     * |        |
     * |        |
     * {@link #startVertX} ---------- {@link #endVertX}
     * 0,0
     */

    @Unique
    private VertexInfo startVertX;
    @Unique
    private VertexInfo endVertX;


    @Unique
    private VertexInfo startVertZ;
    @Unique
    private VertexInfo endVertZ;


    @WrapOperation(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/block/FluidRenderer;vertex(Lnet/minecraft/client/render/VertexConsumer;DDDFFFFFI)V",
                    ordinal = 0
            ),
            method = "render"
    )
    /**
     * targets:
     *       this.vertex(vertexConsumer, d + 0.0, e + (double)p, w + 0.0, ak, an, ao, z, aa, am);
     *   after this:
     *       this.vertex(vertexConsumer, d + 0.0, e + (double)r, w + 1.0, ak, an, ao, ab, ac, am);
     */
    private void waves$addVerticesBetween(FluidRenderer renderer,
                                          VertexConsumer vertexConsumer,
                                          double x, double y, double z,
                                          float red, float green, float blue,
                                          float u, float v,
                                          int light,
                                          Operation<Void> callback
    ) {
        startVertX = new VertexInfo(
                x, y, z,
                red, green, blue,
                u, v,
                light
        );
    }

    @WrapOperation(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/block/FluidRenderer;vertex(Lnet/minecraft/client/render/VertexConsumer;DDDFFFFFI)V",
                    ordinal = 1
            ),
            method = "render"
    )
    /**
     * Targets:
     *   this.vertex(vertexConsumer, d + 0.0, e + (double)r, w + 1.0, ak, an, ao, ab, ac, am);
     * after this:
     *   this.vertex(vertexConsumer, d + 1.0, e + (double)q, w + 1.0, ak, an, ao, ad, ae, am);
     */
    private void waves$addVerticesBetween2(FluidRenderer renderer,
                                           VertexConsumer vertexConsumer,
                                           double x, double y, double z,
                                           float red, float green, float blue,
                                           float u, float v,
                                           int light,
                                           Operation<Void> callback
    ) {
        startVertZ = new VertexInfo(
                x, y, z,
                red, green, blue,
                u, v,
                light
        );
    }

    @WrapOperation(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/block/FluidRenderer;vertex(Lnet/minecraft/client/render/VertexConsumer;DDDFFFFFI)V",
                    ordinal = 2
            ),
            method = "render"
    )
    /**
     * Targets: this.vertex(vertexConsumer, d + 1.0, e + (double)q, w + 1.0, ak, an, ao, ad, ae, am);
     * After: this.vertex(vertexConsumer, d + 1.0, e + (double)o, w + 0.0, ak, an, ao, af, ag, am);
     */
    private void waves$addVerticesBetween3(FluidRenderer renderer,
                                           VertexConsumer vertexConsumer,
                                           double x, double y, double z,
                                           float red, float green, float blue,
                                           float u, float v,
                                           int light,
                                           Operation<Void> callback
    ) {
        endVertZ = new VertexInfo(
                x, y, z,
                red, green, blue,
                u, v,
                light
        );
    }

    @WrapOperation(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/block/FluidRenderer;vertex(Lnet/minecraft/client/render/VertexConsumer;DDDFFFFFI)V",
                    ordinal = 3
            ),
            method = "render"
    )
    /**
     * Targets: this.vertex(vertexConsumer, d + 1.0, e + (double)o, w + 0.0, ak, an, ao, af, ag, am);
     * After: Nothing
     */
    private void waves$addVerticesBetween4(FluidRenderer renderer,
                                           VertexConsumer vertexConsumer,
                                           double x, double y, double z,
                                           float red, float green, float blue,
                                           float u, float v,
                                           int light,
                                           Operation<Void> callback
    ) {
        endVertX = new VertexInfo(
                x, y, z,
                red, green, blue,
                u, v,
                light
        );

        Consumer<VertexInfo> addVertex = vert -> callback.call(
                renderer,
                vertexConsumer,
                vert.x(), vert.y(), vert.z(),
                vert.red(), vert.green(), vert.blue(),
                vert.u(), vert.v(),
                vert.light()
        );

        Debug.renderVertices(
                startVertX, endVertX,
                startVertZ, endVertZ,
                addVertex
        );
    }
}