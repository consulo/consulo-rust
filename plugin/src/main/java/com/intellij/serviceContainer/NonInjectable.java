package com.intellij.serviceContainer;
import java.lang.annotation.*;
/** IntelliJ-compat stub — marks ctors not to be used by DI. */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface NonInjectable {}
