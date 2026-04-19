/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.toml.RsTomlBundle;
import org.toml.lang.psi.*;
import org.toml.lang.psi.ext.TomlLiteralKind;
import org.toml.lang.psi.ext.TomlLiteralKt;

import java.util.List;

public class TomlInvalidKeywordSegmentInspection extends TomlLocalInspectionToolBase {
    @Nullable
    @Override
    protected PsiElementVisitor buildVisitorInternal(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new TomlVisitor() {
            @Override
            public void visitKeyValue(@NotNull TomlKeyValue element) {
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
                        Object kind = TomlLiteralKt.getKind((TomlLiteral) keyword);
                        if (!(kind instanceof TomlLiteralKind.String)) continue;
                        String val = ((TomlLiteralKind.String) kind).getValue();
                        if (val != null && !isValidKeyword(val)) {
                            holder.registerProblem(keyword, RsTomlBundle.message("rust.invalid.keyword"));
                        }
                    }
                }
            }
        };
    }

    private static boolean isValidKeyword(@NotNull String s) {
        if (s.isEmpty() || s.length() > 20) return false;
        if (!Character.isLetter(s.charAt(0))) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') return false;
        }
        return true;
    }
}
