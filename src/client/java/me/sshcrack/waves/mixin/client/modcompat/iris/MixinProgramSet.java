package me.sshcrack.waves.mixin.client.modcompat.iris;

import me.sshcrack.waves.OceanShaders;
import me.sshcrack.waves.ProgramSourceOverridable;
import net.coderbot.iris.shaderpack.ProgramSet;
import net.coderbot.iris.shaderpack.ProgramSource;
import net.coderbot.iris.shaderpack.ShaderPack;
import net.coderbot.iris.shaderpack.ShaderProperties;
import net.coderbot.iris.shaderpack.include.AbsolutePackPath;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Function;
import java.util.stream.Collectors;

@Mixin(ProgramSet.class)
public class MixinProgramSet {
    @Shadow
    @Final
    private ProgramSource gbuffersWater;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void waves$addShaders(
            AbsolutePackPath directory,
            Function<AbsolutePackPath, String> sourceProvider,
            ShaderProperties shaderProperties,
            ShaderPack pack,
            CallbackInfo ci
    ) {
        OceanShaders.beforeProcessingVertex = gbuffersWater
                .getVertexSource()
                .orElse(null);

        try (InputStream in = getClass().getResourceAsStream("/shader/gbuffers_waves.vsh");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

            var fullShader = reader.lines()
                    .collect(Collectors.joining("\n"));

            ((ProgramSourceOverridable) gbuffersWater).waves$setVertexShaderOverride(fullShader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
