/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.parser;

import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsElementTypes;

public final class ParserUtil {

    private ParserUtil() {
    }

    @NotNull
    public static PsiBuilder createRustPsiBuilder(@NotNull Project project, @NotNull CharSequence text) {
        ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(RsLanguage.INSTANCE);
        if (parserDefinition == null) {
            throw new IllegalStateException("No parser definition for language " + RsLanguage.INSTANCE);
        }
        Lexer lexer = parserDefinition.createLexer(project);
        return PsiBuilderFactory.getInstance().createBuilder(parserDefinition, lexer, text);
    }

    /**
     * Creates {@link PsiBuilder} suitable for Grammar Kit generated methods.
     */
    @NotNull
    public static PsiBuilder createAdaptedRustPsiBuilder(@NotNull Project project, @NotNull CharSequence text) {
        PsiBuilder b = GeneratedParserUtilBase.adapt_builder_(
            RsElementTypes.FUNCTION,
            createRustPsiBuilder(project, text),
            new RustParser(),
            RustParser.EXTENDS_SETS_
        );
        // Equivalent to `GeneratedParserUtilBase.enter_section_`.
        // Allows to call `RustParser.*` methods without entering the section
        GeneratedParserUtilBase.ErrorState.get(b).currentFrame = new GeneratedParserUtilBase.Frame();
        return b;
    }

    /**
     * Executes the action with a probe mark that gets rolled back afterwards.
     */
    public static <T> T probe(@NotNull PsiBuilder builder, @NotNull java.util.function.Supplier<T> action) {
        PsiBuilder.Marker mark = builder.mark();
        try {
            return action.get();
        } finally {
            mark.rollbackTo();
        }
    }

    /**
     * Executes the action and rolls back if it returns false.
     */
    public static boolean rollbackIfFalse(@NotNull PsiBuilder builder, @NotNull java.util.function.BooleanSupplier action) {
        PsiBuilder.Marker mark = builder.mark();
        if (action.getAsBoolean()) {
            return true;
        } else {
            mark.rollbackTo();
            return false;
        }
    }

    /**
     * Closes a marker: drops it if result is true, rolls it back if false.
     */
    public static boolean closeMarker(@NotNull PsiBuilder.Marker marker, boolean result) {
        if (result) {
            marker.drop();
        } else {
            marker.rollbackTo();
        }
        return result;
    }

    public static void clearFrame(@NotNull PsiBuilder builder) {
        GeneratedParserUtilBase.ErrorState state = GeneratedParserUtilBase.ErrorState.get(builder);
        GeneratedParserUtilBase.Frame currentFrame = state.currentFrame;
        if (currentFrame != null) {
            currentFrame.errorReportedAt = -1;
            currentFrame.lastVariantAt = -1;
        }
    }

    /**
     * Similar to {@link com.intellij.lang.PsiBuilderUtil#rawTokenText}.
     */
    @NotNull
    public static CharSequence rawLookupText(@NotNull PsiBuilder builder, int steps) {
        int start = builder.rawTokenTypeStart(steps);
        int end = builder.rawTokenTypeStart(steps + 1);
        if (start == -1 || end == -1) {
            return "";
        }
        return builder.getOriginalText().subSequence(start, end);
    }
}
