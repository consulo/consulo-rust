/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiNamedElement;

/**
 * Base interface for named Rust PSI elements.
 */
public interface RsNamedElement extends RsElement, PsiNamedElement, NavigatablePsiElement {
}
