/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.RenameFix;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.Collections;
import java.util.List;

/**
 * Base class for naming inspections. Implements the core logic of checking names
 * and registering problems.
 */
public abstract class RsNamingInspection extends RsLintInspection {
    private final String myElementType;
    private final String myStyleName;
    private final String myElementTitle;

    protected RsNamingInspection(@NotNull String elementType, @NotNull String styleName, @NotNull String elementTitle) {
        myElementType = elementType;
        myStyleName = styleName;
        myElementTitle = elementTitle;
    }

    protected RsNamingInspection(@NotNull String elementType, @NotNull String styleName) {
        this(elementType, styleName, elementType);
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return RsBundle.message("0.naming.convention", myElementTitle);
    }

    @Override
    public boolean isSyntaxOnly() {
        return true;
    }

    public void inspect(@Nullable PsiElement id, @NotNull RsProblemsHolder holder, boolean fix) {
        if (id == null) return;
        String name = RsElementUtil.getUnescapedText(id);
        String suggestedName = checkName(name);
        if (suggestedName == null) return;

        PsiElement fixEl = id.getParent();
        List<RenameFix> fixes = fix && fixEl instanceof PsiNamedElement
            ? Collections.singletonList(new RenameFix((PsiNamedElement) fixEl, suggestedName))
            : Collections.emptyList();

        registerLintProblem(
            holder,
            id,
            RsBundle.message("inspection.message.should.have.case.name.such.as", myElementType, name, myStyleName, suggestedName),
            RsLintHighlightingType.DEFAULT,
            Collections.unmodifiableList(fixes)
        );
    }

    public void inspect(@Nullable PsiElement id, @NotNull RsProblemsHolder holder) {
        inspect(id, holder, true);
    }

    /**
     * Suggests how to rename given name according to the corresponding naming convention.
     * Returns null if the name matches the naming convention.
     */
    @VisibleForTesting
    @Nullable
    public abstract String checkName(@NotNull String name);

    // ---- Utility methods for case conversion ----

    public static boolean hasCase(char c) {
        return Character.isLowerCase(c) || Character.isUpperCase(c);
    }

    public static boolean canStartWord(char c) {
        return Character.isUpperCase(c) || Character.isDigit(c);
    }

    @NotNull
    public static String toCamelCase(@NotNull String s) {
        StringBuilder result = new StringBuilder(s.length());
        boolean wasUnderscore = true;
        boolean startWord = true;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '_') {
                wasUnderscore = true;
            } else if (wasUnderscore || (startWord && canStartWord(ch))) {
                result.append(Character.toUpperCase(ch));
                wasUnderscore = false;
                startWord = false;
            } else {
                startWord = Character.isLowerCase(ch);
                result.append(Character.toLowerCase(ch));
            }
        }
        return result.toString();
    }

    public static boolean isCamelCase(@NotNull String s) {
        if (s.isEmpty()) return false;
        if (Character.isLowerCase(s.charAt(0))) return false;
        if (s.contains("__")) return false;
        for (int i = 0; i < s.length() - 1; i++) {
            char fst = s.charAt(i);
            char snd = s.charAt(i + 1);
            if ((hasCase(fst) && snd == '_') || (hasCase(snd) && fst == '_')) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    public static String toSnakeCase(@NotNull String s, boolean upper) {
        StringBuilder result = new StringBuilder(s.length() + 3);
        // Preserve prefix of underscores and apostrophes
        int prefixLen = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '_' || c == '\'') {
                result.append(c);
                prefixLen++;
            } else {
                break;
            }
        }
        String remaining = s.substring(prefixLen);
        String[] parts = remaining.split("_", -1);
        boolean firstPart = true;
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!firstPart) {
                result.append('_');
            }
            firstPart = false;
            boolean newWord = false;
            boolean firstWord = true;
            for (int i = 0; i < part.length(); i++) {
                char ch = part.charAt(i);
                if (newWord && Character.isUpperCase(ch)) {
                    if (!firstWord) {
                        result.append('_');
                    }
                    newWord = false;
                } else {
                    newWord = Character.isLowerCase(ch);
                }
                result.append(upper ? Character.toUpperCase(ch) : Character.toLowerCase(ch));
                firstWord = false;
            }
        }
        return result.toString();
    }
}
