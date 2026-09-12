/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.codeVision;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.codeVision.CodeVisionRelativeOrdering;
import consulo.language.editor.impl.codeVision.ReferencesCodeVisionProvider;
import consulo.application.util.registry.Registry;
import consulo.application.util.registry.RegistryValue;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.PsiSearchHelper;
import consulo.content.scope.SearchScope;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.RsSearchableUtil;

@ExtensionImpl
public class RsReferenceCodeVisionProvider extends ReferencesCodeVisionProvider {

    public static final String ID = "rust.references";

    private static final RegistryValue CODE_VISION_USAGE_KEY = Registry.get("org.rust.code.vision.usage");
    private static final RegistryValue CODE_VISION_USAGE_SLOW_KEY = Registry.get("org.rust.code.vision.usage.slow");

    @Override
    public boolean acceptsFile(@Nonnull PsiFile file) {
        return file instanceof RsFile;
    }

    @Override
    public boolean acceptsElement(@Nonnull PsiElement element) {
        if (!CODE_VISION_USAGE_KEY.asBoolean(true)) return false;

        if (element instanceof RsAbstractable) {
            if (((RsAbstractable) element).getOwnerBySyntaxOnly().isImplOrTrait()) {
                return CODE_VISION_USAGE_SLOW_KEY.asBoolean();
            }
            return true;
        }
        if (element instanceof RsNamedFieldDecl || element instanceof RsEnumVariant || element instanceof RsMacroDefinitionBase) {
            return CODE_VISION_USAGE_SLOW_KEY.asBoolean();
        }
        if (element instanceof RsStructOrEnumItemElement || element instanceof RsTraitItem
            || element instanceof RsTraitAlias || element instanceof RsModItem || element instanceof RsModDeclItem) {
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public String getHint(@Nonnull PsiElement element, @Nonnull PsiFile file) {
        RsNamedElement namedElement;
        if (element instanceof RsModDeclItem) {
            PsiElement resolved = ((RsModDeclItem) element).getReference().resolve();
            if (resolved instanceof RsFile) {
                namedElement = (RsFile) resolved;
            } else {
                return null;
            }
        } else if (element instanceof RsNamedElement) {
            namedElement = (RsNamedElement) element;
        } else {
            return null;
        }

        String name = namedElement.getName();
        if (name == null) return null;

        PsiSearchHelper searchHelper = PsiSearchHelper.getInstance(namedElement.getProject());
        SearchScope useScope = searchHelper.getUseScope(namedElement);
        if (useScope instanceof GlobalSearchScope) {
            PsiSearchHelper.SearchCostResult searchCost = searchHelper.isCheapEnoughToSearch(name, (GlobalSearchScope) useScope, file, null);
            if (searchCost == PsiSearchHelper.SearchCostResult.TOO_MANY_OCCURRENCES) {
                return null;
            }
        }

        int usageCount = org.rust.lang.core.psi.ext.RsSearchableUtil.searchReferences(namedElement, useScope).size();

        if (element instanceof RsModDeclItem && usageCount > 0) {
            usageCount--;
        }

        if (usageCount == 0) return null;

        return RsBundle.message("rust.code.vision.usage.hint", usageCount);
    }

    @Nonnull
    @Override
    public List<CodeVisionRelativeOrdering> getRelativeOrderings() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String getId() {
        return ID;
    }
}
