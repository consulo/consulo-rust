/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.ide.presentation;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.highlight.HighlightUsagesDescriptionLocation;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.editor.refactoring.util.RefactoringDescriptionLocation;
import consulo.language.psi.ElementDescriptionLocation;
import consulo.language.psi.ElementDescriptionProvider;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.usage.*;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.presentation.PresentationInfo;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

@ExtensionImpl
public class RsDescriptionProvider implements ElementDescriptionProvider {
    @Override
    @RequiredReadAction
    public LocalizeValue getElementDescription(@Nonnull PsiElement element, @Nonnull ElementDescriptionLocation location) {
        if (location instanceof UsageViewNodeTextLocation
            || location instanceof UsageViewShortNameLocation
            || location instanceof UsageViewLongNameLocation
            || location instanceof HighlightUsagesDescriptionLocation) {
            return defaultDescription(element);
        }
        else if (location instanceof UsageViewTypeLocation) {
            if (element instanceof RsNamedElement rsNamedElement) {
                PresentationInfo info = PresentationInfo.getPresentationInfo(rsNamedElement);
                return info != null ? LocalizeValue.ofNullable(info.getType()) : LocalizeValue.empty();
            }
            return LocalizeValue.empty();
        }
        else if (location instanceof RefactoringDescriptionLocation refLoc) {
            return refactoringDescription(element, refLoc.includeParent());
        }
        return LocalizeValue.empty();
    }

    @RequiredReadAction
    private static LocalizeValue defaultDescription(@Nonnull PsiElement element) {
        if (element instanceof RsMod mod) {
            return LocalizeValue.ofNullable(mod.getModName());
        }
        else if (element instanceof RsNamedElement named) {
            return LocalizeValue.ofNullable(named.getName());
        }
        return LocalizeValue.empty();
    }

    @RequiredReadAction
    private static LocalizeValue refactoringDescription(@Nonnull PsiElement element, boolean includeParent) {
        String type = UsageViewUtil.getType(element);
        LocalizeValue elementName = defaultDescription(element);
        if (elementName.isEmpty()) {
            return LocalizeValue.empty();
        }

        PsiElement parent = element.getParent();
        LocalizeValue name;
        if (includeParent && element instanceof RsMod mod) {
            LocalizeValue qName =
                mod instanceof RsQualifiedNamedElement ? LocalizeValue.ofNullable(mod.qualifiedName()) : LocalizeValue.empty();
            name = qName.orIfEmpty(elementName);
        }
        else if (includeParent && parent instanceof RsMod parentMod) {
            LocalizeValue parentQName = parentMod instanceof RsQualifiedNamedElement
                ? LocalizeValue.ofNullable(parentMod.qualifiedName())
                : LocalizeValue.empty();
            name = parentQName.isNotEmpty() ? LocalizeValue.join(parentQName, LocalizeValue.of("::"), elementName) : elementName;
        }
        else {
            name = elementName;
        }
        return LocalizeValue.join(LocalizeValue.of(type), LocalizeValue.space(), name.map(CommonRefactoringUtil::htmlEmphasize));
    }
}
