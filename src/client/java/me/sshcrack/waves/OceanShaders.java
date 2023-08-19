package me.sshcrack.waves;

import io.github.douira.glsl_transformer.ast.print.PrintType;
import io.github.douira.glsl_transformer.ast.transform.ASTInjectionPoint;
import io.github.douira.glsl_transformer.ast.transform.SingleASTTransformer;
import net.coderbot.iris.Iris;
import net.coderbot.iris.gl.uniform.UniformHolder;
import net.coderbot.iris.gl.uniform.UniformUpdateFrequency;
import net.coderbot.iris.shaderpack.DimensionId;
import net.coderbot.iris.shaderpack.IdMap;
import net.coderbot.iris.shaderpack.PackDirectives;
import net.coderbot.iris.uniforms.FrameUpdateNotifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Pair;
import org.joml.Vector3d;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class OceanShaders {
    private static final List<Pair<String, String>> REPLACEMENTS = Stream.of(
            new Pair<>("gl_Vertex", "waves_vertex"),
            new Pair<>("gl_Position", "waves_position"),
            new Pair<>("ftransform()", "waves_position"),
            new Pair<>("gl_Normal", "waves_normal")
    ).toList();


    public static String transformVertexShader(String originalVertex) {
        var modified = originalVertex;
        for (Pair<String, String> pair : REPLACEMENTS) {
            modified = modified.replaceAll(pair.getLeft(), pair.getRight());
        }

        List<String> lines;
        try (InputStream in = WavesModClient.class.getResourceAsStream("/shader/gbuffers_waves.vsh");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            var tempLines = new ArrayList<String>();
            reader.lines().forEach(s -> {
                if (s.startsWith("//"))
                    return;

                if (s.contains("//"))
                    s = s.split("//")[0];

                tempLines.add(s);
            });

            lines = tempLines;
            // Environment is not available here, so we're just not preprocessing the source (although this might come in handy)
            //JcppProcessor.glslPreprocessSource(lines)
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        var nodes = groupLinesInNodes(lines);
        var transformer = new SingleASTTransformer<>();
        transformer.setTransformation((translationUnit, root) -> {
            translationUnit.parseAndInjectNodes(transformer, ASTInjectionPoint.BEFORE_DECLARATIONS, nodes.toArray(String[]::new));
            translationUnit.prependMainFunctionBody(transformer, "compute(gl_Vertex);");
            translationUnit.appendMainFunctionBody(transformer, "gl_Position = waves_position;");
        });

        transformer.setPrintType(PrintType.SIMPLE);
        return transformer.transform(modified);
    }

    private static List<String> groupLinesInNodes(List<String> lines) {
        var bracketsInFunction = 0;
        StringBuilder nextNode = new StringBuilder();

        var grouped = new ArrayList<String>();
        for (String line : lines) {
            if (line.isBlank())
                continue;

            if (line.contains("{"))
                bracketsInFunction++;

            if (line.contains("}"))
                bracketsInFunction--;

            nextNode.append(line);
            if (bracketsInFunction == 0) {
                grouped.add(nextNode.toString());
                nextNode = new StringBuilder();
                continue;
            }
        }

        return grouped;
    }


    private static Vector3d cachedCameraPos = new Vector3d();
    private final static MinecraftClient client = MinecraftClient.getInstance();

    private static void update() {
        var vanillaPos = client.gameRenderer.getCamera().getPos();

        cachedCameraPos.set(vanillaPos.getX(), vanillaPos.getY(), vanillaPos.getZ());
    }


    private static int getWorldDayTime() {
        var world = client.world;
        assert world != null;

        long timeOfDay = world.getTimeOfDay();

        if (Iris.getCurrentDimension() == DimensionId.END || Iris.getCurrentDimension() == DimensionId.NETHER) {
            // If the dimension is the nether or the end, don't override the fixed time.
            // This was an oversight in versions before and including 1.2.5 causing inconsistencies, such as Complementary's ender beams not moving.
            return (int) (timeOfDay % 24000L);
        }

        long dayTime = world.getDimension().fixedTime()
                .orElse(timeOfDay % 24000L);

        return (int) dayTime;
    }


    public static void addNonDynamicUniforms(UniformHolder uniforms, IdMap idMap, PackDirectives directives, FrameUpdateNotifier updateNotifier) {
        updateNotifier.addListener(OceanShaders::update);
        uniforms.
                uniform3d(UniformUpdateFrequency.PER_FRAME, "waves_cameraPosition", () -> cachedCameraPos)
                .uniform1i(UniformUpdateFrequency.PER_TICK, "waves_time", OceanShaders::getWorldDayTime);
    }
}
