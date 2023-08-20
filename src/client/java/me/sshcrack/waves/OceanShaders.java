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
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import org.apache.commons.lang3.RegExUtils;
import org.joml.Vector3d;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.regex.Pattern.compile;

public class OceanShaders {
    private static final String VERTEX_SHADER = "gbuffers_waves.vsh";
    private static final String FRAGMENT_SHADER = "gbuffers_waves.fsh";

    private static Pattern variable(String variableName) {
        return compile(String.format("%s(?![A-z])", variableName));
    }

    private static final List<Pair<Pattern, String>> REPLACEMENTS = Stream.of(
            new Pair<>(variable("gl_Vertex"), "waves_vertex"),
            //new Pair<>(variable("gl_Color"), "waves_color"),
            new Pair<>(variable("gl_Position"), "waves_position"),
            new Pair<>(compile("ftransform\\(\\)"), "waves_position"),
            new Pair<>(variable("gl_Normal"), "waves_normal")
    ).toList();


    public static String transformVertexShader(String originalVertex) {
        var modified = originalVertex;
        for (var pair : REPLACEMENTS) {
            modified = RegExUtils.replaceAll(modified, pair.getLeft(), pair.getRight());
        }

        List<String> lines = readShaderFile(VERTEX_SHADER);
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

    private static List<String> readShaderFile(String path) {
        boolean debug = true;

        if (debug)
            MinecraftClient.getInstance().player
                    .sendMessage(
                            Text.literal("Warning: Debug mode is enabled.")
                                    .setStyle(Style.EMPTY.withColor(Formatting.YELLOW))
                    );

        var readPath = String.format("/shader/%s", path);


        try (
                InputStream in = debug ?
                        new FileInputStream(readPath.substring(1)) :
                        WavesModClient.class.getResourceAsStream(readPath)
                ;
                BufferedReader reader = new BufferedReader(new InputStreamReader(in))
        ) {
            var lines = new ArrayList<String>();
            reader.lines().forEach(s -> {
                if (s.startsWith("//"))
                    return;

                if (s.contains("//"))
                    s = s.split("//")[0];

                lines.add(s);
            });

            return lines;
            // Environment is not available here, so we're just not preprocessing the source (although this might come in handy)
            //JcppProcessor.glslPreprocessSource(lines)
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
