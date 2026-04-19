/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils;

import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.infer.TypeVisitor;
import org.rust.lang.core.types.infer.TypeInference;
import org.rust.lang.core.types.RawTypeUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyTypeParameter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Holds type parameters, lifetimes, const parameters and where clauses.
 * It serves as a combination of several {@code RsGenericDeclaration}s.
 *
 * Can be filtered by a set of types/type references to only return parameters/constraints that are needed by these
 * given types/type references.
 */
public final class ExtractUtil {

    private ExtractUtil() {
    }

    @NotNull
    public static <T> String joinToGenericListString(@NotNull List<T> list, @NotNull java.util.function.Function<T, String> transform) {
        if (list.isEmpty()) return "";
        return list.stream()
            .map(transform)
            .collect(Collectors.joining(", ", "<", ">"));
    }
}
