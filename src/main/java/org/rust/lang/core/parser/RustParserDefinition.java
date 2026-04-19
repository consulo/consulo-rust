/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.LanguageUtil;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.console.RsConsoleCodeFragmentContext;
import org.rust.ide.console.RsConsoleView;
import org.rust.lang.RsDebugInjectionListener;
import org.rust.lang.core.lexer.RsLexer;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.lang.core.stubs.RsPathStub;
import org.rust.lang.doc.psi.RsDocCommentElementType;
import org.rust.lang.doc.psi.ext.RsDocIElementTypeExt;

import static org.rust.lang.core.psi.RsTokenType.RS_ALL_STRING_LITERALS;
import static org.rust.lang.core.psi.RsTokenType.RS_COMMENTS;

public class RustParserDefinition implements ParserDefinition {

    @NotNull
    public static final RsTokenType BLOCK_COMMENT = new RsTokenType("<BLOCK_COMMENT>");
    @NotNull
    public static final RsTokenType EOL_COMMENT = new RsTokenType("<EOL_COMMENT>");
    @NotNull
    public static final RsDocCommentElementType INNER_BLOCK_DOC_COMMENT = new RsDocCommentElementType("<INNER_BLOCK_DOC_COMMENT>");
    @NotNull
    public static final RsDocCommentElementType OUTER_BLOCK_DOC_COMMENT = new RsDocCommentElementType("<OUTER_BLOCK_DOC_COMMENT>");
    @NotNull
    public static final RsDocCommentElementType INNER_EOL_DOC_COMMENT = new RsDocCommentElementType("<INNER_EOL_DOC_COMMENT>");
    @NotNull
    public static final RsDocCommentElementType OUTER_EOL_DOC_COMMENT = new RsDocCommentElementType("<OUTER_EOL_DOC_COMMENT>");

    /**
     * Should be increased after any change of lexer rules
     */
    public static final int LEXER_VERSION = 6;

    /**
     * Should be increased after any change of parser rules
     */
    public static final int PARSER_VERSION = LEXER_VERSION + 51;

    @NotNull
    @Override
    public PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        Project project = viewProvider.getManager().getProject();
        PsiElement injectionHost = InjectedLanguageManager.getInstance(project).getInjectionHost(viewProvider);

        if (injectionHost != null) {
            // this class is contained in clion.jar, so it cannot be used inside `instanceof` type check
            if (!"GDBExpressionPlaceholder".equals(injectionHost.getClass().getSimpleName())) {
                return new RsFile(viewProvider);
            }

            RsDebugInjectionListener injectionListener = project.getMessageBus().syncPublisher(RsDebugInjectionListener.INJECTION_TOPIC);
            RsDebugInjectionListener.DebugContext contextResult = new RsDebugInjectionListener.DebugContext();
            injectionListener.evalDebugContext((PsiLanguageInjectionHost) injectionHost, contextResult);
            RsElement context = contextResult.getElement();
            if (context == null) {
                return new RsFile(viewProvider);
            }

            // RsDebuggerExpressionCodeFragment is not available; fall back to RsFile
            injectionListener.didInject((PsiLanguageInjectionHost) injectionHost);

            return new RsFile(viewProvider);
        } else if (RsConsoleView.VIRTUAL_FILE_NAME.equals(viewProvider.getVirtualFile().getName())) {
            RsBlock context = RsConsoleCodeFragmentContext.createContext(project, null);
            RsReplCodeFragment fragment = new RsReplCodeFragment(viewProvider);
            fragment.setContext(context);
            return fragment;
        }
        return new RsFile(viewProvider);
    }

    @NotNull
    @Override
    public SpaceRequirements spaceExistenceTypeBetweenTokens(@NotNull ASTNode left, @NotNull ASTNode right) {
        if (left.getElementType() == EOL_COMMENT) {
            return SpaceRequirements.MUST_LINE_BREAK;
        }
        if (RsDocIElementTypeExt.isDocCommentLeafToken(left.getElementType())) {
            if (RsDocIElementTypeExt.isDocCommentLeafToken(right.getElementType())) {
                return SpaceRequirements.MAY;
            }
            /** See {@link org.rust.lang.doc.psi.RsDocLinkDestination} */
            if (right.getTreeParent() != null && right.getTreeParent().getElementType() == RsPathStub.Type) {
                return SpaceRequirements.MAY;
            }
            return SpaceRequirements.MUST_LINE_BREAK;
        }
        if (RsDocIElementTypeExt.isDocCommentLeafToken(right.getElementType())
            && left.getTreeParent() != null && left.getTreeParent().getElementType() == RsPathStub.Type) {
            return SpaceRequirements.MAY;
        }
        return LanguageUtil.canStickTokensTogetherByLexer(left, right, new RsLexer());
    }

    @NotNull
    @Override
    public IFileElementType getFileNodeType() {
        return RsFileStub.Type;
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return RS_ALL_STRING_LITERALS;
    }

    @NotNull
    @Override
    public TokenSet getWhitespaceTokens() {
        return TokenSet.create(TokenType.WHITE_SPACE);
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return RS_COMMENTS;
    }

    @NotNull
    @Override
    public PsiElement createElement(@NotNull ASTNode node) {
        return RsElementTypes.Factory.createElement(node);
    }

    @NotNull
    @Override
    public Lexer createLexer(@Nullable Project project) {
        return new RsLexer();
    }

    @NotNull
    @Override
    public PsiParser createParser(@Nullable Project project) {
        return new RustParser();
    }
}
