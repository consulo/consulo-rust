/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

/**
 * A problem found by the language layer while analysing Rust code, such as a type mismatch or a use
 * of a compiler feature that the current toolchain does not provide.
 * <p>
 * Implementations carry only the facts about the problem: the element it belongs to and the values
 * involved. They say nothing about severity, error codes, messages or quick fixes - deciding how a
 * problem is shown to the user is the job of the IDE layer, which maps these types onto its own
 * presentable diagnostics.
 */
public interface RsInferenceDiagnostic {
    /** The element this problem is attached to. */
    @Nonnull
    PsiElement getElement();
}
