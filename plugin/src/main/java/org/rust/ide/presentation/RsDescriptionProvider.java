/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.presentation;

import consulo.language.editor.highlight.HighlightUsagesDescriptionLocation;
import consulo.language.psi.ElementDescriptionLocation;
import consulo.language.psi.ElementDescriptionProvider;
import consulo.language.psi.PsiElement;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.editor.refactoring.util.RefactoringDescriptionLocation;
import consulo.usage.UsageInfo;
import consulo.usage.UsageViewDescriptor;
import consulo.usage.UsageViewUtil;
import consulo.usage.UsageViewLongNameLocation;
import consulo.usage.UsageViewNodeTextLocation;
import consulo.usage.UsageViewShortNameLocation;
import consulo.usage.UsageViewTypeLocation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

public class RsDescriptionProvider implements ElementDescriptionProvider {

    @Nullable
    @Override
    public String getElementDescription(@Nonnull PsiElement element, @Nonnull ElementDescriptionLocation location) {
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
    private static String defaultDescription(@Nonnull PsiElement element) {
        if (element instanceof RsMod mod) {
            return mod.getModName();
        } else if (element instanceof RsNamedElement named) {
            return named.getName();
        }
        return null;
    }

    @Nullable
    private static String refactoringDescription(@Nonnull PsiElement element, boolean includeParent) {
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
