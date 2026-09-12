/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.toml.RsTomlBundle;
import org.toml.lang.psi.*;
import org.toml.lang.psi.ext.TomlLiteralKind;
import org.toml.lang.psi.ext.TomlLiteralExt;

import java.util.List;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.localize.LocalizeValue;

@ExtensionImpl
public class TomlInvalidKeywordSegmentInspection extends TomlLocalInspectionToolBase {
    @Nullable
    @Override
    protected PsiElementVisitor buildVisitorInternal(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        return new TomlVisitor() {
            @Override
            public void visitKeyValue(@Nonnull TomlKeyValue element) {
                List<TomlKeySegment> segments = element.getKey().getSegments();
                if (segments.size() != 1) return;
                if (!"keywords".equals(segments.get(0).getName())) return;

                TomlValue value = element.getValue();
                if (!(value instanceof TomlArray)) return;
                TomlArray keywordsArray = (TomlArray) value;
                List<? extends PsiElement> keywords = keywordsArray.getElements();
                if (keywords.size() > 5) {
                    holder.registerProblem(keywordsArray, RsTomlBundle.message("rust.too.many.keywords"));
                }
                for (PsiElement keyword : keywords) {
                    if (keyword instanceof TomlLiteral) {
                        Object kind = TomlLiteralExt.getKind((TomlLiteral) keyword);
                        if (!(kind instanceof TomlLiteralKind.StringKind)) continue;
                        String val = ((TomlLiteralKind.StringKind) kind).getValue();
                        if (val != null && !isValidKeyword(val)) {
                            holder.registerProblem(keyword, RsTomlBundle.message("rust.invalid.keyword"));
                        }
                    }
                }
            }
        };
    }

    private static boolean isValidKeyword(@Nonnull String s) {
        if (s.isEmpty() || s.length() > 20) return false;
        if (!Character.isLetter(s.charAt(0))) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') return false;
        }
        return true;
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.toml.invalid.keyword.segment.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("cargo.toml"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }
}
