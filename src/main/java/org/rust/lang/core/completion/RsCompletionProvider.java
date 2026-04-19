/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;

public abstract class RsCompletionProvider extends CompletionProvider<CompletionParameters> {
    public abstract ElementPattern<? extends PsiElement> getElementPattern();
}
