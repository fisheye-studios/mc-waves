package me.sshcrack.waves;

import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuad;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadViewMutable;
import me.jellysquid.mods.sodium.client.model.quad.blender.ColorSampler;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadWinding;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import me.sshcrack.waves.mixin.client.VertexInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.function.ToFloatFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Spline;
import net.minecraft.world.BlockRenderView;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public class Debug {

    private static final Spline<Float, ToFloatFunction<Float>> CUT_SPLINE = Spline.builder(ToFloatFunction.fromFloat(v -> v))
            .add(MathHelper.square(0f), 10)
            .add(MathHelper.square(5f), 3)
            .add(MathHelper.square(10f), 2)
            .add(MathHelper.square(20f), 1)
            .add(MathHelper.square(30f), 0)
            .build();

    private static Vector3fc getCoordinates(int vertexIndex, ModelQuadView quad) {
        return new Vector3f(
                quad.getX(vertexIndex),
                quad.getY(vertexIndex),
                quad.getZ(vertexIndex)
        );
    }

    public static void splitWater(
            FluidRenderer renderer,
            ModelQuadViewMutable quad,
            BlockRenderView world,
            BlockPos pos,
            LightPipeline lighter,
            Direction dir,
            float brightness,
            ColorSampler<FluidState> colorSampler,
            FluidState fluidState,
            ChunkModelBuilder buffers,
            BlockPos offset
    ) {
        var acc = (FluidRendererState) renderer;
        var cam = MinecraftClient.getInstance().getCameraEntity();

        if(cam == null) {
            // something is wrong here
            acc.waves$updateQuad(quad, world, pos, lighter, dir, brightness, colorSampler, fluidState);
            acc.waves$writeQuad(buffers, offset, quad, ModelQuadFacing.UP, ModelQuadWinding.CLOCKWISE);

            return;
        }



        var squaredDistance = cam.getBlockPos().getSquaredDistance(pos);
        int cuts = MathHelper.floor(CUT_SPLINE.apply((float) squaredDistance));
        var quadDimension = cuts + 1;

        // the vertices of the width and the height are of this length
        int vertexDimension = quadDimension * 2;
        float singleVertex = 1f / vertexDimension;

        float singleQuad = 1f / quadDimension;

        var startVertX = SodiumVertexInfo.of(quad, 0);
        var startVertZ = SodiumVertexInfo.of(quad, 1);
        var endVertZ = SodiumVertexInfo.of(quad, 2);
        var endVertX = SodiumVertexInfo.of(quad, 3);

        var rand = (float) Math.random() * .5f;
        for (float currQuadX = 0; currQuadX < quadDimension; currQuadX++) {
            float startX = (currQuadX * 2f) * singleVertex;
            for (float currQuadZ = 0; currQuadZ < quadDimension; currQuadZ++) {
                var startZ = (currQuadZ * 2f) * singleVertex;

                var endX = startX + singleQuad;
                var endZ = startZ + singleQuad;

                var quadOffset = (float) Math.random() * .1f;
                DebugFunction<SodiumVertexInfo, Integer, Float, Float> setVertex = (i, x, z) -> {
                    var currSide = startVertX.lerp(endVertX, x);
                    var otherSide = startVertZ.lerp(endVertZ, x);

                    var vertex = currSide.lerp(otherSide, z);
                    setVertex(quad, i, vertex.x(), vertex.y() + rand + quadOffset, vertex.z(), vertex.u(), vertex.v());

                    return vertex;
                };

                // d1 - d4 just there for debugging purposes
                var d1 = setVertex.accept(0, startX, startZ);
                var d2 = setVertex.accept(1, startX, endZ);
                var d3 = setVertex.accept(2, endX, endZ);
                var d4 = setVertex.accept(3, endX, startZ);


                acc.waves$updateQuad(quad, world, pos, lighter, dir, brightness, colorSampler, fluidState);

                // replaced "facing" with ModelQuadFacing.DOWN
                acc.waves$writeQuad(buffers, offset, quad, ModelQuadFacing.UP, ModelQuadWinding.CLOCKWISE);
            }
        }
        //acc.waves$updateQuad(quad, world, pos, lighter, dir, brightness, colorSampler, fluidState);

        // replaced "facing" with ModelQuadFacing.DOWN
        //acc.waves$writeQuad(buffers, offset, quad, ModelQuadFacing.UP, ModelQuadWinding.CLOCKWISE);
    }

    public static void renderVertices(
            VertexInfo startVertX, VertexInfo endVertX,
            VertexInfo startVertZ, VertexInfo endVertZ,
            Consumer<VertexInfo> addVertex
    ) {
        double SUBDIVIDE = 0;

        var totalVertices = SUBDIVIDE + 1;
        for (double currX = 0; currX <= totalVertices; currX++) {
            boolean isEven = currX % 2 == 0;

            var startZ = isEven ? 0 : totalVertices;
            var eachStep = isEven ? 1.0 : -1.0;

            var deltaX = currX / totalVertices;
            var from = startVertX.lerp(endVertX, deltaX);
            var to = startVertZ.lerp(endVertZ, deltaX);

            // this is so the for loop produces this pattern for adding vertices:
            // |------------- |
            //                |
            // |------------- |
            // |
            // |------------- etc. (ignore the spaces)

            for (double currZ = startZ; currZ <= totalVertices; currZ *= eachStep) {
                var deltaZ = currZ / totalVertices;

                var totalLerped = from.lerp(to, deltaZ);
                addVertex.accept(totalLerped);
            }
        }
    }

    private static void setVertex(ModelQuadViewMutable quad, int i, float x, float y, float z, float u, float v) {
        /*
        if(x != 0 && x != 1)
            x -= 0.001F;
        if(y != 0 && y != 1)
            y -= 0.001F;
        if(z != 0 && z != 1)
            z -= 0.001F;
        */
        quad.setX(i, x);
        quad.setY(i, y);
        quad.setZ(i, z);
        quad.setTexU(i, u);
        quad.setTexV(i, v);
    }
}


/**
 * 0.0, 0.8, 0.0
 * 0.0, 0.8, 1.0
 * 1.0, 0.8, 1.0
 * 1.0, 0.8, 0.0
 * <p>
 * <p>
 * output:
 * 0.0,0.08,0.0
 * 1.0, 0.18, 0
 * 1.0, 0.99, 1.0
 * 0.0, 0.012, 1.0
 */