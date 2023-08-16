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
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public class Debug {
    private static ModelQuadViewMutable quad = new ModelQuad();

    private static Vector3fc getCoordinates(int vertexIndex, ModelQuadView quad) {
        return new Vector3f(
                quad.getX(vertexIndex),
                quad.getY(vertexIndex),
                quad.getZ(vertexIndex)
        );
    }

    public static void splitWater(
            FluidRenderer renderer,
            ModelQuadView quadMethod,
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
        //acc.waves$updateQuad(quadMethod, world, pos, lighter, Direction.UP, 1.0F, colorSampler, fluidState);
        //acc.waves$writeQuad(buffers, offset, quadMethod, ModelQuadFacing.UP, ModelQuadWinding.CLOCKWISE);

        quad.setFlags(0);
        quad.setSprite(quadMethod.getSprite());

        int cuts = 1;
        var quadDimension = cuts + 1;

        // the vertices of the width and the height are of this length
        int vertexDimension = quadDimension * 2;
        double singleVertex = 1d / vertexDimension;

        double singleQuad = 1d / quadDimension;

        var startVertX = SodiumVertexInfo.of(quadMethod, 0);
        var startVertZ = SodiumVertexInfo.of(quadMethod, 1);
        var endVertZ = SodiumVertexInfo.of(quadMethod, 2);
        var endVertX = SodiumVertexInfo.of(quadMethod, 3);


        for (double currQuadX = 0; currQuadX < quadDimension; currQuadX++) {
            double startX = (float) ((currQuadX * 2d) * singleVertex);
            for (double currQuadZ = 0; currQuadZ < quadDimension; currQuadZ++) {
                var startZ = (currQuadZ * 2d) * singleVertex;

                var endX = startX + singleQuad;
                var endZ = startZ + singleQuad;

                TripleConsumer<Integer, Double, Double> setVertex = (i, x, z) -> {
                    var currSide = startVertX.lerp(endVertX, x);
                    var otherSide = startVertZ.lerp(endVertZ, x);

                    var vertex = currSide.lerp(otherSide, z);
                    setVertex(quad, i, vertex.x(), vertex.y(), vertex.z(), vertex.u(), vertex.v());
                };

                setVertex.accept(0, startX, startZ);
                setVertex.accept(1, startX, endZ);
                setVertex.accept(2, endX, endZ);
                setVertex.accept(3, endX, startZ);

                acc.waves$updateQuad(quad, world, pos, lighter, dir, brightness, colorSampler, fluidState);

                // replaced "facing" with ModelQuadFacing.DOWN
                acc.waves$writeQuad(buffers, offset, quad, ModelQuadFacing.UP, ModelQuadWinding.CLOCKWISE);

            }
        }
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

    private static void setVertex(ModelQuadViewMutable quad, int i, double x, double y, double z, double u, double v) {
        quad.setX(i, (float) x);
        quad.setY(i, (float) y);
        quad.setZ(i, (float) z);
        quad.setTexU(i, (float) u);
        quad.setTexV(i, (float) v);
    }
}
