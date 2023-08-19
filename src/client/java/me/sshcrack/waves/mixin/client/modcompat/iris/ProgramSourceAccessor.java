package me.sshcrack.waves.mixin.client.modcompat.iris;

import me.sshcrack.waves.ProgramSourceOverridable;
import net.coderbot.iris.shaderpack.ProgramSource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = ProgramSource.class, remap = false)
public class ProgramSourceAccessor implements ProgramSourceOverridable {
    @Unique
    @Nullable
    private String vertexOverride;

    @Override
    public void waves$setVertexShaderOverride(String vertex) {
        vertexOverride = vertex;
    }

    @Inject(method = "getVertexSource", at = @At("HEAD"), cancellable = true)
    private void waves$injectWaves(CallbackInfoReturnable<Optional<String>> cir) {
        if (vertexOverride != null)
            cir.setReturnValue(Optional.of(vertexOverride));
    }

}
