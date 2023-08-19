package me.sshcrack.waves.mixin.client.modcompat.iris;

import me.sshcrack.waves.OceanShaders;
import me.sshcrack.waves.interfaces.ProgramSourceOverridable;
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

import java.util.function.Function;

@Mixin(value = ProgramSet.class, remap = false)
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
        var vertex = gbuffersWater.getVertexSource().orElse(null);
        if(vertex == null)
            return;

        ((ProgramSourceOverridable) gbuffersWater)
                .waves$setVertexShaderOverride(
                        OceanShaders.transformVertexShader(vertex)
                );
    }
}
