/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.presentation;

import com.intellij.codeInsight.highlighting.HighlightUsagesDescriptionLocation;
import com.intellij.psi.ElementDescriptionLocation;
import com.intellij.psi.ElementDescriptionProvider;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.refactoring.util.RefactoringDescriptionLocation;
import com.intellij.usageView.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

public class RsDescriptionProvider implements ElementDescriptionProvider {

    @Nullable
    @Override
    public String getElementDescription(@NotNull PsiElement element, @NotNull ElementDescriptionLocation location) {
        if (location instanceof UsageViewNodeTextLocation
            || location instanceof UsageViewShortNameLocation
            || location instanceof UsageViewLongNameLocation
            || location instanceof HighlightUsagesDescriptionLocation) {
            return defaultDescription(element);
        } else if (location instanceof UsageViewTypeLocation) {
            if (element instanceof RsNamedElement rsNamedElement) {
                PresentationInfo info = PresentationInfo.getPresentationInfo(rsNamedElement);
                return info != null ? info.getType() : null;
            }
            return null;
        } else if (location instanceof RefactoringDescriptionLocation refLoc) {
            return refactoringDescription(element, refLoc.includeParent());
        }
        return null;
    }

    @Nullable
    private static String defaultDescription(@NotNull PsiElement element) {
        if (element instanceof RsMod mod) {
            return mod.getModName();
        } else if (element instanceof RsNamedElement named) {
            return named.getName();
        }
        return null;
    }

    @Nullable
    private static String refactoringDescription(@NotNull PsiElement element, boolean includeParent) {
        String type = UsageViewUtil.getType(element);
        String elementName = defaultDescription(element);
        if (elementName == null) return null;

        PsiElement parent = element.getParent();
        String name;
        if (includeParent && element instanceof RsMod mod) {
            String qName = mod instanceof RsQualifiedNamedElement ? ((RsQualifiedNamedElement) mod).qualifiedName() : null;
            name = qName != null ? qName : elementName;
        } else if (includeParent && parent instanceof RsMod parentMod) {
            String parentQName = parentMod instanceof RsQualifiedNamedElement ? ((RsQualifiedNamedElement) parentMod).qualifiedName() : null;
            name = parentQName != null ? RsBundle.message("0.1", parentQName, elementName) : elementName;
        } else {
            name = elementName;
        }
        return type + " " + CommonRefactoringUtil.htmlEmphasize(name);
    }
}
