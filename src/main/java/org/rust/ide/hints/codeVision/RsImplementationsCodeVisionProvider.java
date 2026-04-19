/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.codeVision;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering;
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator;
import com.intellij.codeInsight.hints.codeVision.InheritorsCodeVisionProvider;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.EmptyQuery;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.RsPsiUtilUtil;
import org.rust.openapiext.OpenApiUtil;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.RsElement;

public class RsImplementationsCodeVisionProvider extends InheritorsCodeVisionProvider {

    public static final String ID = "rust.inheritors";

    private static final Key<CachedValue<Integer>> IMPL_CACHE_KEY = Key.create("IMPL_CACHE_KEY");

    @Override
    public boolean acceptsFile(@NotNull PsiFile file) {
        return file instanceof RsFile;
    }

    @Override
    public boolean acceptsElement(@NotNull PsiElement element) {
        if (!OpenApiUtil.isUnitTestMode() && !Registry.is("org.rust.code.vision.implementation", false)) return false;

        if (element instanceof RsTraitItem) {
            return true;
        }
        if (element instanceof RsAbstractable) {
            return ((RsAbstractable) element).getOwner() instanceof RsAbstractableOwner.Trait;
        }
        return false;
    }

    @Nullable
    @Override
    public String getHint(@NotNull PsiElement element, @NotNull PsiFile file) {
        if (!(element instanceof RsElement)) return null;
        if (!(element instanceof RsTraitItem) && !(element instanceof RsAbstractable)) return null;

        int implementationCount = getImplementationCount((RsElement) element);
        if (implementationCount == 0) return null;

        if (element instanceof RsTraitItem) {
            return RsBundle.message("rust.code.vision.implementation.hint", implementationCount);
        }
        if (element instanceof RsAbstractable) {
            if (((RsAbstractable) element).isAbstract()) {
                return RsBundle.message("rust.code.vision.implementation.hint", implementationCount);
            } else {
                return RsBundle.message("rust.code.vision.overrides.hint", implementationCount);
            }
        }
        return null;
    }

    @Override
    public void handleClick(@NotNull Editor editor, @NotNull PsiElement element, @Nullable MouseEvent event) {
        if (event == null) return;
        if (!(element instanceof RsNamedElement)) return;
        String elementName = ((RsNamedElement) element).getName();
        if (elementName == null) return;

        List<PsiElement> impls = getImplementations(element);
        List<NavigatablePsiElement> navigatable = new ArrayList<>();
        for (PsiElement impl : impls) {
            if (impl instanceof NavigatablePsiElement) {
                navigatable.add((NavigatablePsiElement) impl);
            }
        }
        if (navigatable.isEmpty()) return;

        NavigatablePsiElement[] targets = navigatable.toArray(new NavigatablePsiElement[0]);
        String escapedName = StringUtil.escapeXmlEntities(elementName);

        PsiElementListNavigator.openTargets(
            event,
            targets,
            CodeInsightBundle.message("goto.implementation.chooserTitle", escapedName, targets.length, ""),
            CodeInsightBundle.message("goto.implementation.findUsages.title", escapedName, targets.length),
            new DefaultPsiElementCellRenderer()
        );
    }

    @NotNull
    @Override
    public List<CodeVisionRelativeOrdering> getRelativeOrderings() {
        return Collections.singletonList(new CodeVisionRelativeOrdering.CodeVisionRelativeOrderingAfter(RsReferenceCodeVisionProvider.ID));
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @NotNull
    private static Query<? extends PsiElement> getImplementationsQuery(@NotNull PsiElement element) {
        if (element instanceof RsTraitItem) {
            return RsTraitItemUtil.searchForImplementations((RsTraitItem) element);
        }
        if (element instanceof RsAbstractable) {
            java.util.List<RsAbstractable> impls = RsAbstractableUtil.searchForImplementations((RsAbstractable) element);
            return new com.intellij.util.CollectionQuery<>(new java.util.ArrayList<>(impls));
        }
        return new EmptyQuery<>();
    }

    @NotNull
    private static List<PsiElement> getImplementations(@NotNull PsiElement element) {
        return new ArrayList<>(getImplementationsQuery(element).findAll());
    }

    private static int countImplementations(@NotNull PsiElement element) {
        return getImplementations(element).size();
    }

    private static int getImplementationCount(@NotNull RsElement element) {
        return CachedValuesManager.getCachedValue(element, IMPL_CACHE_KEY, () -> {
            int usages = countImplementations(element);
            return CachedValueProvider.Result.create(usages, RsPsiUtilUtil.getRustStructureModificationTracker(element.getProject()));
        });
    }
}
