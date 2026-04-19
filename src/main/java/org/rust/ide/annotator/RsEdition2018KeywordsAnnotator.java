/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.colors.RsColor;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.openapiext.OpenApiUtil;

import java.util.HashSet;
import java.util.Set;

public class RsEdition2018KeywordsAnnotator extends AnnotatorBase {

    private static final Set<String> EDITION_2018_RESERVED_NAMES = new HashSet<>();
    static {
        EDITION_2018_RESERVED_NAMES.add("async");
        EDITION_2018_RESERVED_NAMES.add("await");
        EDITION_2018_RESERVED_NAMES.add("try");
    }

    @Override
    protected void annotateInternal(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (RsElementUtil.getEdition(element) == null) return;

        if (!isEdition2018Keyword(element)) return;

        boolean isAtLeastEdition2018 = RsElementUtil.isAtLeastEdition2018(element);
        boolean isIdentifier = RsElementUtil.getElementType(element) == RsElementTypes.IDENTIFIER;
        boolean isEnabledByCfg = RsPsiElementExt.isEnabledByCfg(element);

        if (isAtLeastEdition2018 && isIdentifier && isNameIdentifier(element)) {
            holder.newAnnotation(HighlightSeverity.ERROR, RsBundle.message("inspection.message.reserved.keyword.in.edition", element.getText())).create();
        } else if (isAtLeastEdition2018 && !isIdentifier && isEnabledByCfg) {
            if (!holder.isBatchMode()) {
                HighlightSeverity severity = OpenApiUtil.isUnitTestMode() ? RsColor.KEYWORD.getTestSeverity() : HighlightSeverity.INFORMATION;
                holder.newSilentAnnotation(severity)
                    .textAttributes(RsColor.KEYWORD.getTextAttributesKey()).create();
            }
        } else if (isAtLeastEdition2018 && !isIdentifier && !isEnabledByCfg) {
            if (!holder.isBatchMode()) {
                EditorColorsManager colorsManager = EditorColorsManager.getInstance();
                TextAttributes keywordTextAttributes = colorsManager.getGlobalScheme().getAttributes(RsColor.KEYWORD.getTextAttributesKey());
                TextAttributes cfgDisabledCodeTextAttributes = colorsManager.getGlobalScheme().getAttributes(RsColor.CFG_DISABLED_CODE.getTextAttributesKey());
                TextAttributes cfgDisabledKeywordTextAttributes = TextAttributes.merge(keywordTextAttributes, cfgDisabledCodeTextAttributes);

                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .enforcedTextAttributes(cfgDisabledKeywordTextAttributes).create();
            }
        } else if (!isAtLeastEdition2018 && !isIdentifier) {
            holder.newAnnotation(HighlightSeverity.ERROR, RsBundle.message("inspection.message.this.feature.only.available.in.edition")).create();
        }
    }

    public static boolean isEdition2018Keyword(@NotNull PsiElement element) {
        boolean isReservedIdentifier = RsElementUtil.getElementType(element) == RsElementTypes.IDENTIFIER
            && EDITION_2018_RESERVED_NAMES.contains(element.getText())
            && !(element.getParent() instanceof RsMacro)
            && !(element.getParent() != null && element.getParent().getParent() instanceof RsMacroCall)
            && !(element.getParent() instanceof RsFieldLookup);
        boolean isEdition2018Keyword = RsTokenType.RS_EDITION_2018_KEYWORDS.contains(RsElementUtil.getElementType(element));
        return (isReservedIdentifier || isEdition2018Keyword)
            && PsiTreeUtil.getParentOfType(element, RsUseItem.class, RsMetaItemArgs.class) == null;
    }

    public static boolean isNameIdentifier(@NotNull PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent instanceof RsReferenceElement && element == ((RsReferenceElement) parent).getReferenceNameElement()) {
            return true;
        }
        if (parent instanceof RsNameIdentifierOwner && element == ((RsNameIdentifierOwner) parent).getNameIdentifier()) {
            return true;
        }
        return false;
    }
}
