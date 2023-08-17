package me.sshcrack.waves;

@FunctionalInterface
public interface DebugFunction<A, T, U, K> {
    A accept(T t, U u, K k);
}
