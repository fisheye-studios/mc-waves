package me.sshcrack.waves.mixin.client.modcompat.iris;

import me.sshcrack.waves.OceanShaders;
import net.coderbot.iris.gl.uniform.UniformHolder;
import net.coderbot.iris.shaderpack.IdMap;
import net.coderbot.iris.shaderpack.PackDirectives;
import net.coderbot.iris.uniforms.CommonUniforms;
import net.coderbot.iris.uniforms.FrameUpdateNotifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CommonUniforms.class, remap = false)
public class MixinCommonUniforms {
    @Inject(method = "addNonDynamicUniforms", at = @At("TAIL"))
    private static void waves$addUniforms(UniformHolder uniforms, IdMap idMap, PackDirectives directives, FrameUpdateNotifier updateNotifier, CallbackInfo ci) {
        OceanShaders.addNonDynamicUniforms(uniforms, idMap, directives, updateNotifier);
    }
}
