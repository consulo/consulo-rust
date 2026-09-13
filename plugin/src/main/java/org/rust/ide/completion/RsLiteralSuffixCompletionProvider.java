/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.lang.core.psi.impl.RsLiteralKind;
import org.rust.lang.core.psi.impl.RsLiteralKindUtil;
import org.rust.lang.core.types.ty.TyFloat;
import org.rust.lang.core.types.ty.TyInteger;

import java.util.ArrayList;
import java.util.List;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;
import consulo.language.pattern.PatternCondition;
import org.rust.lang.core.completion.RsCompletionProvider;
import org.rust.lang.core.completion.Utils;

public class RsLiteralSuffixCompletionProvider extends RsCompletionProvider {
    public static final RsLiteralSuffixCompletionProvider INSTANCE = new RsLiteralSuffixCompletionProvider();

    private RsLiteralSuffixCompletionProvider() {
    }

    @Nonnull
    @Override
    public ElementPattern<PsiElement> getElementPattern() {
        return psiElement(PsiElement.class)
            .withParent(psiElement(RsLitExpr.class).with(new consulo.language.pattern.PatternCondition<PsiElement>("isLiteralNumberWithoutExistingSuffix") {
                @Override
                public boolean accepts(@Nonnull PsiElement psi, ProcessingContext ctx) {
                    if (!(psi instanceof RsLitExpr)) return false;
                    RsLitExpr litExpr = (RsLitExpr) Utils.getOriginalOrSelf(psi);
                    RsLiteralKind kind = RsLiteralKindUtil.getKind(litExpr);
                    String suffix;
                    if (kind instanceof RsLiteralKind.IntegerLiteral) {
                        suffix = ((RsLiteralKind.IntegerLiteral) kind).getSuffix();
                    } else if (kind instanceof RsLiteralKind.FloatLiteral) {
                        suffix = ((RsLiteralKind.FloatLiteral) kind).getSuffix();
                    } else {
                        return false;
                    }
                    if (suffix == null) return false;
                    List<String> allNames = new ArrayList<>();
                    allNames.addAll(TyInteger.NAMES);
                    allNames.addAll(TyFloat.NAMES);
                    for (String name : allNames) {
                        if (suffix.contains(name)) return false;
                    }
                    return true;
                }
            }));
    }

    @Override
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
    ) {
        String literal = Utils.getOriginalOrSelf(parameters.getPosition()).getText();
        for (String suffix : getValidSuffixes(literal)) {
            tryToCompleteSuffix(suffix, literal, result);
        }
    }

    private static List<String> getValidSuffixes(String literal) {
        if (literal.contains(".") || literal.contains("e")) {
            return new ArrayList<>(TyFloat.NAMES);
        }
        if (literal.startsWith("0b") || literal.startsWith("0o")) {
            return new ArrayList<>(TyInteger.NAMES);
        }
        List<String> all = new ArrayList<>();
        all.addAll(TyInteger.NAMES);
        all.addAll(TyFloat.NAMES);
        return all;
    }

    private static void tryToCompleteSuffix(String suffix, String literal, CompletionResultSet result) {
        for (int i = 1; i <= suffix.length(); i++) {
            String suffixPrefix = suffix.substring(0, i);
            if (literal.endsWith(suffixPrefix)) {
                result.addElement(LookupElementBuilder.create(literal + suffix.substring(suffixPrefix.length())));
                break;
            }
        }
    }
}
