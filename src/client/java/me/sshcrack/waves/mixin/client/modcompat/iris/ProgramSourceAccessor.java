package me.sshcrack.waves.mixin.client.modcompat.iris;

import net.coderbot.iris.shaderpack.ProgramSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(ProgramSource.class)
public interface ProgramSourceAccessor {
    @Accessor("vertexSource")
    void setVertexSource(String vertexSource);
}
