/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.language.parser.PsiBuilder;
import consulo.project.Project;
import consulo.language.ast.IElementType;
import consulo.util.collection.SmartList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.macros.decl.MacroExpansionMarks;
import org.rust.lang.core.parser.ParserUtil;
import org.rust.lang.doc.psi.RsDocKind;
import consulo.util.lang.Pair;

import java.util.ArrayList;
import java.util.List;
import org.rust.lang.core.psi.RsTokenSets;

public final class DocsLowering {
    private DocsLowering() {}

    @Nonnull
    public static Pair<PsiBuilder, RangeMap> lowerDocCommentsToAdaptedPsiBuilder(
        @Nonnull PsiBuilder builder,
        @Nonnull Project project
    ) {
        Pair<CharSequence, RangeMap> lowered = lowerDocComments(builder);
        if (lowered == null) {
            return new Pair<>(builder, defaultRangeMap(builder));
        }
        return new Pair<>(
            ParserUtil.createAdaptedRustPsiBuilder(project, lowered.getFirst()),
            lowered.getSecond()
        );
    }

    @Nonnull
    public static Pair<PsiBuilder, RangeMap> lowerDocCommentsToPsiBuilder(
        @Nonnull PsiBuilder builder,
        @Nonnull Project project
    ) {
        Pair<CharSequence, RangeMap> lowered = lowerDocComments(builder);
        if (lowered == null) {
            return new Pair<>(builder, defaultRangeMap(builder));
        }
        return new Pair<>(
            ParserUtil.createRustPsiBuilder(project, lowered.getFirst()),
            lowered.getSecond()
        );
    }

    @Nonnull
    private static RangeMap defaultRangeMap(@Nonnull PsiBuilder builder) {
        if (builder.getOriginalText().length() > 0) {
            return new RangeMap(new MappedTextRange(0, 0, builder.getOriginalText().length()));
        } else {
            return RangeMap.EMPTY;
        }
    }

    /** Rustc replaces doc comments like {@code /// foo} to attributes {@code #[doc = "foo"]} before macro expansion */
    
    @Nullable
    public static Pair<CharSequence, RangeMap> lowerDocComments(@Nonnull PsiBuilder builder) {
        if (!hasDocComments(builder)) {
            return null;
        }

        MacroExpansionMarks.DocsLowering.hit();

        StringBuilder sb = new StringBuilder((int) (builder.getOriginalText().length() * 1.1));
        List<MappedTextRange> ranges = new SmartList<>();

        int i = 0;
        while (true) {
            IElementType token = builder.rawLookup(i);
            if (token == null) break;
            CharSequence text = ParserUtil.rawLookupText(builder, i);
            int start = builder.rawTokenTypeStart(i);
            i++;

            if (RsTokenSets.RS_DOC_COMMENTS.contains(token)) {
                RsDocKind kind = RsDocKind.of(token);
                String attrPrefix = (kind == RsDocKind.InnerBlock || kind == RsDocKind.InnerEol) ? "#!" : "#";
                if (kind.isBlock()) {
                    sb.append(attrPrefix);
                    sb.append("[doc=\"");
                    String content = text.toString();
                    if (content.startsWith(kind.getPrefix())) {
                        content = content.substring(kind.getPrefix().length());
                    }
                    if (content.endsWith(kind.getSuffix())) {
                        content = content.substring(0, content.length() - kind.getSuffix().length());
                    }
                    escapeRust(content, sb);
                    sb.append("\"]\n");
                } else {
                    String[] comments = text.toString().split("\n");
                    for (String comment : comments) {
                        sb.append(attrPrefix);
                        sb.append("[doc=\"");
                        String trimmed = comment.trim();
                        if (trimmed.startsWith(kind.getPrefix())) {
                            trimmed = trimmed.substring(kind.getPrefix().length());
                        }
                        escapeRust(trimmed, sb);
                        sb.append("\"]\n");
                    }
                }
            } else {
                RangeMap.mergeAdd(ranges, new MappedTextRange(start, sb.length(), text.length()));
                sb.append(text);
            }
        }

        return new Pair<>(sb, new RangeMap(ranges));
    }

    /** Escapes special characters in a string for use inside a Rust string literal. */
    private static void escapeRust(@Nonnull String s, @Nonnull StringBuilder sb) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:   sb.append(c); break;
            }
        }
    }

    private static boolean hasDocComments(@Nonnull PsiBuilder builder) {
        int i = 0;
        while (true) {
            IElementType token = builder.rawLookup(i++);
            if (token == null) break;
            if (RsTokenSets.RS_DOC_COMMENTS.contains(token)) return true;
        }
        return false;
    }
}
