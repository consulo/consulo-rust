/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.psi.impl.source.tree.LazyParseableElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IReparseableElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.lexer.RsLexer;
import org.rust.lang.core.parser.RustParserUtil;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;

public final class LazyElements {

    @NotNull
    public static IElementType factory(@NotNull String name) {
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
        public boolean isParsable(@Nullable ASTNode parent, @NotNull CharSequence buffer,
                                  @NotNull Language fileLanguage, @NotNull Project project) {
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
        RsTTBodyLazyElementTypeBase(@NotNull String debugName) {
            super(debugName);
        }

        @Override
        public boolean isReparseable(@NotNull ASTNode currentNode, @NotNull CharSequence newText,
                                     @NotNull Language fileLanguage, @NotNull Project project) {
            return RustParserUtil.hasProperTokenTreeBraceBalance(newText, new RsLexer());
        }
    }

    private static abstract class RsReparseableElementTypeBase extends IReparseableElementType {
        RsReparseableElementTypeBase(@NotNull String debugName) {
            super(debugName, RsLanguage.INSTANCE);
        }

        /**
         * Must be non-null to make re-parsing work.
         * See {@code com.intellij.psi.impl.BlockSupportImpl.tryReparseNode}
         */
        @NotNull
        @Override
        public final ASTNode createNode(@Nullable CharSequence text) {
            return new LazyParseableElement(this, text);
        }
    }

    private LazyElements() {}
}
