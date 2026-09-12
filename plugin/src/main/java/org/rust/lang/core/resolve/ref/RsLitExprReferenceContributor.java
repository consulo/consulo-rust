/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import consulo.ide.impl.idea.openapi.paths.GlobalPathReferenceProvider;
import consulo.language.psi.PsiReferenceContributor;
import consulo.language.psi.PsiReferenceRegistrar;
import consulo.language.impl.psi.path.WebReference;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.StubBasedPsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.ElementManipulator;
import consulo.language.psi.LiteralTextEscaper;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.ContributedReferenceHost;
import consulo.language.psi.SyntaxTraverser;
import consulo.language.psi.path.FileReference;
import consulo.language.psi.path.FileReferenceSet;
import consulo.language.util.ProcessingContext;
import consulo.util.collection.SmartList;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.List;
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
