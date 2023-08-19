package me.sshcrack.waves.debugging;

@FunctionalInterface
public interface DebugFunction<A, T, U, K> {
    A accept(T t, U u, K k);
}
