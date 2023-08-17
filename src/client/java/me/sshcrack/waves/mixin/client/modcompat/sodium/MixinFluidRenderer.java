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
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Spline;
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

    @Shadow @Final private ModelQuadViewMutable quad;

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

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/FluidRenderer;updateQuad(Lme/jellysquid/mods/sodium/client/model/quad/ModelQuadView;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/util/math/BlockPos;Lme/jellysquid/mods/sodium/client/model/light/LightPipeline;Lnet/minecraft/util/math/Direction;FLme/jellysquid/mods/sodium/client/model/quad/blender/ColorSampler;Lnet/minecraft/fluid/FluidState;)V",
                    ordinal = 0
            )
    )
    // wtf is this method haha
    private void wave$subdivideWater(
            FluidRenderer renderer,
            ModelQuadView quad,
            BlockRenderView world,
            BlockPos pos,
            LightPipeline lighter,
            Direction dir,
            float brightness,
            ColorSampler<FluidState> colorSampler,
            FluidState fluidState,

            Operation<Void> operation,

            BlockRenderView _world, FluidState _fluidState, BlockPos _pos, BlockPos offset, ChunkModelBuilder buffers
    ) {
        if (!fluidState.isIn(FluidTags.WATER) || isSubdividing) {
            operation.call(renderer, quad, world, pos, lighter, dir, brightness, colorSampler, fluidState);
            return;
        }

        waves$setSubdividing(true);
        Debug.splitWater(FluidRenderer.class.cast(this), this.quad, world, pos, lighter, Direction.UP, 1.0F, colorSampler, fluidState, buffers, offset);
        waves$setSubdividing(false);
        hasRendered = true;
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/FluidRenderer;writeQuad(Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;Lnet/minecraft/util/math/BlockPos;Lme/jellysquid/mods/sodium/client/model/quad/ModelQuadView;Lme/jellysquid/mods/sodium/client/model/quad/properties/ModelQuadFacing;Lme/jellysquid/mods/sodium/client/model/quad/properties/ModelQuadWinding;)V",
                    ordinal = 1
            )
    )
    private void waves$writeQuad2(
            FluidRenderer renderer,
            ChunkModelBuilder builder, BlockPos offset, ModelQuadView quad, ModelQuadFacing facing, ModelQuadWinding winding,
            Operation<Void> operation
    ) {
        if(!hasRendered)
            operation.call(renderer, builder, offset, quad, facing, winding);
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/FluidRenderer;writeQuad(Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;Lnet/minecraft/util/math/BlockPos;Lme/jellysquid/mods/sodium/client/model/quad/ModelQuadView;Lme/jellysquid/mods/sodium/client/model/quad/properties/ModelQuadFacing;Lme/jellysquid/mods/sodium/client/model/quad/properties/ModelQuadWinding;)V",
                    ordinal = 0
            )
    )
    private void waves$writeQuad(
            FluidRenderer renderer,
            ChunkModelBuilder builder, BlockPos offset, ModelQuadView quad, ModelQuadFacing facing, ModelQuadWinding winding,
            Operation<Void> operation
    ) {
        if(!hasRendered)
            operation.call(renderer, builder, offset, quad, facing, winding);
    }

    @Inject(method = "render", at=@At("TAIL"))
    private void waves$resetRendered(BlockRenderView world, FluidState fluidState, BlockPos pos, BlockPos offset, ChunkModelBuilder buffers, CallbackInfoReturnable<Boolean> cir) {
        hasRendered = false;
    }
}
