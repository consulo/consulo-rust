/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.inject.InjectionBackgroundSuppressor;
import jakarta.annotation.Nullable;

public interface RsDocCodeFence extends RsDocElement, PsiLanguageInjectionHost, InjectionBackgroundSuppressor {
    RsDocCodeFenceStartEnd getStart();
    @Nullable RsDocCodeFenceStartEnd getEnd();
    @Nullable RsDocCodeFenceLang getLang();
}
