/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;

public abstract class RsCompletionProvider implements CompletionProvider {
    public abstract ElementPattern<? extends PsiElement> getElementPattern();
}
