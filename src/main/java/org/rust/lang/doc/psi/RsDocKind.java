/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.parser.RustParserDefinition;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public enum RsDocKind {
    Attr("", ""),
    InnerBlock("/*!", "*"),
    OuterBlock("/**", "*"),
    InnerEol("//!", "//!"),
    OuterEol("///", "///");

    private final String prefix;
    private final String infix;

    RsDocKind(String prefix, String infix) {
        this.prefix = prefix;
        this.infix = infix;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getInfix() {
        return infix;
    }

    public String getSuffix() {
        return isBlock() ? "*/" : "";
    }

    public boolean isBlock() {
        return this == InnerBlock || this == OuterBlock;
    }

    /**
     * Removes doc comment decoration from a text and returns a stream of content lines.
     */
    @NotNull
    public Stream<CharSequence> removeDecoration(@NotNull CharSequence text) {
        // Simplified implementation: strip prefix/suffix and return lines
        String s = text.toString();
        switch (this) {
            case Attr:
                return Stream.of(s);
            case InnerEol:
            case OuterEol: {
                return s.lines().map(line -> {
                    String trimmed = line.stripLeading();
                    if (trimmed.startsWith(infix)) {
                        String rest = trimmed.substring(infix.length());
                        if (rest.startsWith(" ")) rest = rest.substring(1);
                        return rest;
                    }
                    return (CharSequence) line;
                });
            }
            case InnerBlock:
            case OuterBlock: {
                String stripped = s;
                if (stripped.startsWith(prefix)) stripped = stripped.substring(prefix.length());
                if (stripped.endsWith(getSuffix())) stripped = stripped.substring(0, stripped.length() - getSuffix().length());
                return stripped.lines().map(line -> {
                    String trimmed = line.stripLeading();
                    if (trimmed.startsWith("*") && !trimmed.startsWith("*/")) {
                        String rest = trimmed.substring(1);
                        if (rest.startsWith(" ")) rest = rest.substring(1);
                        return (CharSequence) rest;
                    }
                    return (CharSequence) line;
                });
            }
            default:
                return Stream.of(s);
        }
    }

    @NotNull
    public static RsDocKind of(@NotNull IElementType tokenType) {
        if (tokenType == RustParserDefinition.INNER_BLOCK_DOC_COMMENT) return InnerBlock;
        if (tokenType == RustParserDefinition.OUTER_BLOCK_DOC_COMMENT) return OuterBlock;
        if (tokenType == RustParserDefinition.INNER_EOL_DOC_COMMENT) return InnerEol;
        if (tokenType == RustParserDefinition.OUTER_EOL_DOC_COMMENT) return OuterEol;
        throw new IllegalArgumentException("unsupported token type");
    }
}
