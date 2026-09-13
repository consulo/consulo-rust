/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.parser;

import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiBuilderFactory;
import consulo.language.parser.ParserDefinition;
import consulo.language.impl.parser.GeneratedParserUtilBase;
import consulo.language.lexer.Lexer;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsElementTypes;
import consulo.language.version.LanguageVersion;

public final class ParserUtil {

    private ParserUtil() {
    }

    @Nonnull
    public static PsiBuilder createRustPsiBuilder(@Nonnull Project project, @Nonnull CharSequence text) {
        ParserDefinition parserDefinition = ParserDefinition.forLanguage(RsLanguage.INSTANCE);
        if (parserDefinition == null) {
            throw new IllegalStateException("No parser definition for language " + RsLanguage.INSTANCE);
        }
        consulo.language.version.LanguageVersion version = RsLanguage.INSTANCE.getVersions()[0];
        Lexer lexer = parserDefinition.createLexer(version);
        return PsiBuilderFactory.getInstance().createBuilder(parserDefinition, lexer, text);
    }

    /**
     * Creates {@link PsiBuilder} suitable for Grammar Kit generated methods.
     */
    @Nonnull
    public static PsiBuilder createAdaptedRustPsiBuilder(@Nonnull Project project, @Nonnull CharSequence text) {
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
    public static <T> T probe(@Nonnull PsiBuilder builder, @Nonnull java.util.function.Supplier<T> action) {
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
    public static boolean rollbackIfFalse(@Nonnull PsiBuilder builder, @Nonnull java.util.function.BooleanSupplier action) {
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
    public static boolean closeMarker(@Nonnull PsiBuilder.Marker marker, boolean result) {
        if (result) {
            marker.drop();
        } else {
            marker.rollbackTo();
        }
        return result;
    }

    public static void clearFrame(@Nonnull PsiBuilder builder) {
        GeneratedParserUtilBase.ErrorState state = GeneratedParserUtilBase.ErrorState.get(builder);
        GeneratedParserUtilBase.Frame currentFrame = state.currentFrame;
        if (currentFrame != null) {
            currentFrame.errorReportedAt = -1;
        }
    }

    /**
     * Similar to {@link consulo.language.parser.PsiBuilderUtil#rawTokenText}.
     */
    @Nonnull
    public static CharSequence rawLookupText(@Nonnull PsiBuilder builder, int steps) {
        int start = builder.rawTokenTypeStart(steps);
        int end = builder.rawTokenTypeStart(steps + 1);
        if (start == -1 || end == -1) {
            return "";
        }
        return builder.getOriginalText().subSequence(start, end);
    }
}
