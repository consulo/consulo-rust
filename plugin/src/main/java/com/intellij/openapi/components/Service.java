/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package com.intellij.openapi.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IntelliJ-compat stub. Consulo uses {@code @ServiceAPI} / {@code @ServiceImpl} to
 * mark services. At runtime, service registration is rewritten — this annotation
 * is retained only so existing source keeps compiling.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Service {
    Level[] value() default {};

    enum Level { APP, PROJECT }
}
