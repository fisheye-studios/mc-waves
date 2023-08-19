package me.sshcrack.waves.util;

import java.util.Objects;
import java.util.function.Consumer;

@FunctionalInterface
public interface TripleConsumer<T, U, K> {
    void accept(T t, U u, K k);
}
