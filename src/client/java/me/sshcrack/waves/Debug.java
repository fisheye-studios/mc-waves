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
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.sshcrack.waves.mixin.client.VertexInfo;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public class Debug {
    private static final ModelQuadViewMutable quad = new ModelQuad();


    static {
        quad.setNormal(Norm3b.pack(0.0F, 1.0F, 0.0F));
    }

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
        //acc.waves$updateQuad(quadMethod, world, pos, lighter, dir, brightness, colorSampler, fluidState);
        //acc.waves$writeQuad(buffers, offset, quadMethod, ModelQuadFacing.UP, ModelQuadWinding.CLOCKWISE);


        quad.setFlags(0);
        quad.setSprite(quadMethod.getSprite());

        int cuts = 0;
        var quadDimension = cuts + 1;

        // the vertices of the width and the height are of this length
        int vertexDimension = quadDimension * 2;
        float singleVertex = 1f / vertexDimension;

        float singleQuad = 1f / quadDimension;

        // Is inaccurrate

        var _startVertX = SodiumVertexInfo.of(quadMethod, 0);
        var _startVertZ = SodiumVertexInfo.of(quadMethod, 1);
        var _endVertZ = SodiumVertexInfo.of(quadMethod, 2);
        var _endVertX = SodiumVertexInfo.of(quadMethod, 3);


        var y = .8f;
        var startVertX = new SodiumVertexInfo(
                0, y, 0,
                _startVertX.u(),
                _startVertX.v()
        );

        var startVertZ = new SodiumVertexInfo(
                0, y, 1,
                _startVertZ.u(), _startVertZ.v()
        );

        var endVertZ = new SodiumVertexInfo(
                1, y, 1,
                _endVertZ.u(), _endVertZ.v()
        );

        var endVertX = new SodiumVertexInfo(
                1, y, 0,
                _endVertX.u(), _endVertX.v()
        );


        for (float currQuadX = 0; currQuadX < quadDimension; currQuadX++) {
            float startX = (currQuadX * 2f) * singleVertex;
            for (float currQuadZ = 0; currQuadZ < quadDimension; currQuadZ++) {
                var startZ = (currQuadZ * 2f) * singleVertex;

                var endX = startX + singleQuad;
                var endZ = startZ + singleQuad;

                DebugFunction<SodiumVertexInfo, Integer, Float, Float> setVertex = (i, x, z) -> {
                    var currSide = startVertX.lerp(endVertX, x);
                    var otherSide = startVertZ.lerp(endVertZ, x);

                    var vertex = currSide.lerp(otherSide, z);
                    setVertex(quad, i, vertex.x(), vertex.y(), vertex.z(), vertex.u(), vertex.v());

                    return vertex;
                };

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