/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import consulo.language.ast.ASTNode;
import consulo.language.Language;
import consulo.project.Project;
import consulo.language.impl.ast.LazyParseableElement;
import consulo.language.ast.IElementType;
import consulo.language.ast.IReparseableElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.lexer.RsLexer;
import org.rust.lang.core.parser.RustParserUtil;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;

public final class LazyElements {

    @Nonnull
    public static IElementType factory(@Nonnull String name) {
        return switch (name) {
            case "MACRO_ARGUMENT" -> RsMacroArgumentElementType.INSTANCE;
            case "MACRO_BODY" -> RsMacroBodyElementType.INSTANCE;
            default -> throw new IllegalStateException("Unknown element " + name);
        };
    }

    private static final class RsMacroArgumentElementType extends RsReparseableElementTypeBase {
        static final RsMacroArgumentElementType INSTANCE = new RsMacroArgumentElementType();

        private RsMacroArgumentElementType() {
            super("MACRO_ARGUMENT");
        }

        @Override
        public boolean isParsable(@Nullable ASTNode parent, @Nonnull CharSequence buffer,
                                  @Nonnull Language fileLanguage, @Nonnull Project project) {
            if (parent == null) return false;
            var psi = parent.getPsi();
            if (!(psi instanceof RsMacroCall parentMacro)) return false;

            // Special macros are not reparseable because a change in the content of a macro argument
            // can change a type of the argument (e.g. to VEC_MACRO_ARGUMENT)
            if (RustParserUtil.isSpecialMacro(RsMacroCallUtil.getMacroName(parentMacro))) return false;

            return RustParserUtil.hasProperTokenTreeBraceBalance(buffer, new RsLexer());
        }
    }

    private static final class RsMacroBodyElementType extends RsTTBodyLazyElementTypeBase {
        static final RsMacroBodyElementType INSTANCE = new RsMacroBodyElementType();

        private RsMacroBodyElementType() {
            super("MACRO_BODY");
        }
    }

    private static abstract class RsTTBodyLazyElementTypeBase extends RsReparseableElementTypeBase {
        RsTTBodyLazyElementTypeBase(@Nonnull String debugName) {
            super(debugName);
        }

        public boolean isReparseable(@Nonnull ASTNode currentNode, @Nonnull CharSequence newText,
                                     @Nonnull Language fileLanguage, @Nonnull Project project) {
            return RustParserUtil.hasProperTokenTreeBraceBalance(newText, new RsLexer());
        }
    }

    private static abstract class RsReparseableElementTypeBase extends IReparseableElementType {
        RsReparseableElementTypeBase(@Nonnull String debugName) {
            super(debugName, RsLanguage.INSTANCE);
        }

        /**
         * Must be non-null to make re-parsing work.
         * See {@code com.intellij.psi.impl.BlockSupportImpl.tryReparseNode}
         */
        @Nonnull
        @Override
        public final ASTNode createNode(@Nullable CharSequence text) {
            return new LazyParseableElement(this, text);
        }
    }

    private LazyElements() {}
}
