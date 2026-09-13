/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move.common;

import org.rust.ide.refactoring.move.common.RsMoveUtil;
import consulo.project.Project;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.imports.RsImportHelper;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.psi.ext.impl.*;

public class RsMovePathHelper {

    @Nonnull
    private final Project project;
    @Nonnull
    private final RsMod mod;
    @Nonnull
    private final RsCodeFragmentFactory codeFragmentFactory;
    @Nullable
    private final RsQualifiedNamedElement existingPublicItem;

    public RsMovePathHelper(@Nonnull Project project, @Nonnull RsMod mod) {
        this.project = project;
        this.mod = mod;
        this.codeFragmentFactory = new RsCodeFragmentFactory(project);
        this.existingPublicItem = findExistingPublicItem();
    }

    @Nullable
    private RsQualifiedNamedElement findExistingPublicItem() {
        List<RsQualifiedNamedElement> items = RsElementUtil.childrenOfType(mod, RsQualifiedNamedElement.class);
        return items.stream()
            .filter(item -> item instanceof RsVisibilityOwner
                && ((RsVisibilityOwner) item).getVisibility() == RsVisibility.Public.INSTANCE
                && item.getName() != null)
            .sorted(Comparator.comparingInt(item -> -(item.getName() != null ? item.getName().length() : 0)))
            .filter(item -> {
                Collection<PsiReference> itemUsages = ReferencesSearch.search(item, GlobalSearchScope.projectScope(project)).findAll();
                return itemUsages.stream().noneMatch(ref -> {
                    RsUseItem useItem = PsiTreeUtil.getParentOfType(ref.getElement(), RsUseItem.class);
                    return useItem != null && useItem.getVis() != null;
                });
            })
            .findFirst()
            .orElse(null);
    }

    @Nullable
    public RsPath findPathAfterMove(@Nonnull RsElement context, @Nonnull RsQualifiedNamedElement element) {
        String elementName = element instanceof RsFile
            ? ((RsFile) element).getModName()
            : element.getName();
        if (elementName == null) return null;
        if (RsElementUtil.getContainingModOrSelf(context) == mod) {
            return codeFragmentFactory.createPath(elementName, context);
        }

        RsPath result = findPathAfterMoveUsingOtherItemInMod(context, elementName);
        return result != null ? result : findPathAfterMoveUsingMod(context, elementName);
    }

    @Nullable
    private RsPath findPathAfterMoveUsingOtherItemInMod(@Nonnull RsElement context, @Nonnull String elementName) {
        if (existingPublicItem == null) return null;
        String secondaryElementName = existingPublicItem.getName();
        if (secondaryElementName == null) return null;
        String secondaryPathText = findPath(context, existingPublicItem);
        if (secondaryPathText == null) return null;
        RsPath secondaryPath = codeFragmentFactory.createPath(secondaryPathText, context);
        if (secondaryPath == null) return null;
        if (secondaryPath.getReference() == null || secondaryPath.getReference().resolve() != existingPublicItem) return null;

        if (!secondaryPathText.endsWith("::" + secondaryElementName)) return null;
        String pathText = secondaryPathText.substring(0, secondaryPathText.length() - secondaryElementName.length()) + elementName;
        return codeFragmentFactory.createPath(pathText, context);
    }

    @Nullable
    private RsPath findPathAfterMoveUsingMod(@Nonnull RsElement context, @Nonnull String elementName) {
        String modPath = findPath(context, mod);
        if (modPath == null) return null;
        String elementPath = modPath + "::" + elementName;
        return codeFragmentFactory.createPath(elementPath, context);
    }

    @Nullable
    private String findPath(@Nonnull RsElement context, @Nonnull RsQualifiedNamedElement element) {
        String pathSimple = findPathSimple(context, element);
        if (pathSimple != null) return pathSimple;

        String path = RsImportHelper.findPath(context, element);
        if (path == null) return null;
        return RsMoveUtil.convertPathToRelativeIfPossible(RsElementUtil.getContainingModOrSelf(context), path);
    }

    @Nullable
    private String findPathSimple(@Nonnull RsElement context, @Nonnull RsQualifiedNamedElement element) {
        RsMod contextMod = RsElementUtil.getContainingModOrSelf(context);
        String pathText = element.getQualifiedNameRelativeTo(contextMod);
        if (pathText == null) return null;
        RsPath path = codeFragmentFactory.createPath(pathText, context);
        if (path == null) return null;
        return RsMoveUtil.resolvesToAndAccessible(path, element) ? path.getText() : null;
    }
}
