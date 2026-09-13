/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.codeVision;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.codeVision.CodeVisionRelativeOrdering;
import consulo.language.editor.impl.codeVision.InheritorsCodeVisionProvider;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.ui.navigation.PsiTargetNavigationService;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.util.dataholder.Key;
import consulo.application.util.registry.Registry;
import consulo.util.lang.StringUtil;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.application.util.query.EmptyQuery;
import consulo.application.util.query.Query;
import consulo.ui.event.ComponentEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.impl.RsPsiUtilUtil;
import org.rust.openapiext.OpenApiUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.RsElement;
import consulo.application.util.query.CollectionQuery;
import org.rust.lang.core.psi.ext.impl.*;

@ExtensionImpl
public class RsImplementationsCodeVisionProvider extends InheritorsCodeVisionProvider {

    public static final String ID = "rust.inheritors";

    private static final Key<CachedValue<Integer>> IMPL_CACHE_KEY = Key.create("IMPL_CACHE_KEY");

    @Override
    public boolean acceptsFile(@Nonnull PsiFile file) {
        return file instanceof RsFile;
    }

    @Override
    public boolean acceptsElement(@Nonnull PsiElement element) {
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
    public String getHint(@Nonnull PsiElement element, @Nonnull PsiFile file) {
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
    public void handleClick(@Nonnull Editor editor, @Nonnull PsiElement element, @Nullable ComponentEvent<?> event) {
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

        String escapedName = StringUtil.escapeXmlEntities(elementName);

        Application.get().getInstance(PsiTargetNavigationService.class)
            .newNavigator(() -> navigatable)
            .title(CodeInsightLocalize.gotoImplementationChoosertitle(escapedName, navigatable.size(), ""))
            .findUsagesTitle(CodeInsightLocalize.gotoImplementationFindusagesTitle(escapedName))
            .navigate(editor, element.getProject());
    }

    @Nonnull
    @Override
    public List<CodeVisionRelativeOrdering> getRelativeOrderings() {
        return Collections.singletonList(new CodeVisionRelativeOrdering.CodeVisionRelativeOrderingAfter(RsReferenceCodeVisionProvider.ID));
    }

    @Nonnull
    @Override
    public String getId() {
        return ID;
    }

    @Nonnull
    private static Query<? extends PsiElement> getImplementationsQuery(@Nonnull PsiElement element) {
        if (element instanceof RsTraitItem) {
            return RsTraitItemUtil.searchForImplementations((RsTraitItem) element);
        }
        if (element instanceof RsAbstractable) {
            java.util.List<RsAbstractable> impls = RsAbstractableUtil.searchForImplementations((RsAbstractable) element);
            return new consulo.application.util.query.CollectionQuery<>(new java.util.ArrayList<>(impls));
        }
        return new EmptyQuery<>();
    }

    @Nonnull
    private static List<PsiElement> getImplementations(@Nonnull PsiElement element) {
        return new ArrayList<>(getImplementationsQuery(element).findAll());
    }

    private static int countImplementations(@Nonnull PsiElement element) {
        return getImplementations(element).size();
    }

    private static int getImplementationCount(@Nonnull RsElement element) {
        return CachedValuesManager.getManager(element.getProject()).getCachedValue(element, IMPL_CACHE_KEY, () -> {
            int usages = countImplementations(element);
            return CachedValueProvider.Result.create(usages, RsPsiUtilUtil.getRustStructureModificationTracker(element.getProject()));
        }, false);
    }
}
