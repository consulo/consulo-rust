package com.intellij.util;

/** IntelliJ-compat Function stub mirroring com.intellij.util.Function. */
@FunctionalInterface
public interface Function<Param, Result> {
    Result fun(Param param);
    interface Mono<T> extends Function<T, T> {}
}
