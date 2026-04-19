/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.impl.source.tree.injected.InjectionBackgroundSuppressor;
import org.jetbrains.annotations.Nullable;

public interface RsDocCodeFence extends RsDocElement, PsiLanguageInjectionHost, InjectionBackgroundSuppressor {
    RsDocCodeFenceStartEnd getStart();
    @Nullable RsDocCodeFenceStartEnd getEnd();
    @Nullable RsDocCodeFenceLang getLang();
}
