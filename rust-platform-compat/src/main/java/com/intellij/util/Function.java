package com.intellij.util;

/** Single-argument function returning a result. */
@FunctionalInterface
public interface Function<Param, Result> {
    Result fun(Param param);
    interface Mono<T> extends Function<T, T> {}
}
