/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.codeVision;

import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering;
import com.intellij.codeInsight.hints.codeVision.ReferencesCodeVisionProvider;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.registry.RegistryValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.Collections;
import java.util.List;

public class RsReferenceCodeVisionProvider extends ReferencesCodeVisionProvider {

    public static final String ID = "rust.references";

    private static final RegistryValue CODE_VISION_USAGE_KEY = Registry.get("org.rust.code.vision.usage");
    private static final RegistryValue CODE_VISION_USAGE_SLOW_KEY = Registry.get("org.rust.code.vision.usage.slow");

    @Override
    public boolean acceptsFile(@NotNull PsiFile file) {
        return file instanceof RsFile;
    }

    @Override
    public boolean acceptsElement(@NotNull PsiElement element) {
        if (!CODE_VISION_USAGE_KEY.asBoolean()) return false;

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
    public String getHint(@NotNull PsiElement element, @NotNull PsiFile file) {
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

    @NotNull
    @Override
    public List<CodeVisionRelativeOrdering> getRelativeOrderings() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }
}
