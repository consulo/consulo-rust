/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import consulo.language.psi.PsiReferenceContributor;
import consulo.language.psi.PsiReferenceRegistrar;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import org.rust.lang.RsLanguage;

@ExtensionImpl
public class RsLitExprReferenceContributor extends PsiReferenceContributor {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }

    public void registerReferenceProviders(@Nonnull PsiReferenceRegistrar registrar) {
        // Registration of reference providers for literal expressions
        // The patterns (includeMacroLiteral, pathAttrLiteral, pathValueLiteral, literal)
    }
}
