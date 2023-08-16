package me.sshcrack.waves.mixin.client.modcompat.sodium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadViewMutable;
import me.jellysquid.mods.sodium.client.model.quad.blender.ColorSampler;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadWinding;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import me.sshcrack.waves.Debug;
import me.sshcrack.waves.FluidRendererState;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = FluidRenderer.class, remap = false)
public abstract class MixinFluidRenderer implements FluidRendererState {
    @Shadow protected abstract void updateQuad(ModelQuadView quad, BlockRenderView world, BlockPos pos, LightPipeline lighter, Direction dir, float brightness, ColorSampler<FluidState> colorSampler, FluidState fluidState);

    @Shadow protected abstract void writeQuad(ChunkModelBuilder builder, BlockPos offset, ModelQuadView quad, ModelQuadFacing facing, ModelQuadWinding winding);

    @Override
    public boolean waves$isSubdividing() {
        return isSubdividing;
    }

    @Override
    public void waves$setSubdividing(boolean subdividing) {
        this.isSubdividing = subdividing;
    }

    @Unique
    private boolean isSubdividing = false;

    @Unique
    private boolean hasRendered = false;

    @Override
    public void waves$updateQuad(ModelQuadView quad, BlockRenderView world, BlockPos pos, LightPipeline lighter, Direction dir, float brightness, ColorSampler<FluidState> colorSampler, FluidState fluidState) {
        updateQuad(quad, world, pos, lighter, dir, brightness, colorSampler, fluidState);
    }

    @Override
    public void waves$writeQuad(ChunkModelBuilder builder, BlockPos offset, ModelQuadView quad, ModelQuadFacing facing, ModelQuadWinding winding) {
        writeQuad(builder, offset, quad, facing, winding);
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/FluidRenderer;updateQuad(Lme/jellysquid/mods/sodium/client/model/quad/ModelQuadView;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/util/math/BlockPos;Lme/jellysquid/mods/sodium/client/model/light/LightPipeline;Lnet/minecraft/util/math/Direction;FLme/jellysquid/mods/sodium/client/model/quad/blender/ColorSampler;Lnet/minecraft/fluid/FluidState;)V",
                    ordinal = 0
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    // wtf is this method haha
    private void wave$subdivideWater(
            BlockRenderView world, FluidState fluidState,
            BlockPos pos, BlockPos offset,
            ChunkModelBuilder buffers, CallbackInfoReturnable<Boolean> cir,


            int posX, int posY, int posZ,
            Fluid fluid,
            boolean sfUp, boolean sfDown, boolean sfNorth, boolean sfSouth, boolean sfWest, boolean sfEast,
            boolean isWater,
            FluidRenderHandler handler,
            ColorSampler<FluidState> colorizer,
            Sprite[] sprites,
            boolean rendered,
            float fluidHeight,
            float h1, float h2, float h3, float h4,
            float yOffset,
            ModelQuadViewMutable quad,
            LightMode lightMode,
            LightPipeline lighter,
            Vec3d velocity,
            Sprite sprite,
            ModelQuadFacing facing,
            float u1, float u2, float u3, float u4,
            float v1, float v2, float v3, float v4,
            float uAvg, float vAvg,
            float s1, float s2, float s3
    ) {
        if (!isWater || isSubdividing) {
            return;
        }

        waves$setSubdividing(true);
        Debug.splitWater(FluidRenderer.class.cast(this), quad, world, pos, lighter, Direction.UP, 1.0F, colorizer, fluidState, buffers, offset, facing);
        waves$setSubdividing(false);
        hasRendered = true;

        cir.cancel();
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/FluidRenderer;writeQuad(Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;Lnet/minecraft/util/math/BlockPos;Lme/jellysquid/mods/sodium/client/model/quad/ModelQuadView;Lme/jellysquid/mods/sodium/client/model/quad/properties/ModelQuadFacing;Lme/jellysquid/mods/sodium/client/model/quad/properties/ModelQuadWinding;)V",
                    ordinal = 0
            )
    )
    private void waves$writeQuad(BlockRenderView world, FluidState fluidState, BlockPos pos, BlockPos offset, ChunkModelBuilder buffers, CallbackInfoReturnable<Boolean> cir) {
        if(!hasRendered)
            return;

        hasRendered = false;
        cir.cancel();
    }
}
