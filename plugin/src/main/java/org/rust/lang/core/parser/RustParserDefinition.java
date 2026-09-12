/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.parser;

import consulo.language.ast.ASTNode;
import consulo.language.util.LanguageUtil;
import consulo.language.parser.ParserDefinition;
import consulo.language.parser.PsiParser;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.lexer.Lexer;
import consulo.project.Project;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.ast.TokenType;
import consulo.language.ast.IFileElementType;
import consulo.language.ast.TokenSet;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.console.RsConsoleCodeFragmentContext;
import org.rust.ide.console.RsConsoleView;
import org.rust.lang.RsDebugInjectionListener;
import org.rust.lang.core.lexer.RsLexer;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.lang.core.stubs.RsPathStub;
import org.rust.lang.doc.psi.ext.RsDocIElementTypeExt;

import static org.rust.lang.core.psi.RsTokenType.EOL_COMMENT;
import static org.rust.lang.core.psi.RsTokenSets.RS_ALL_STRING_LITERALS;
import static org.rust.lang.core.psi.RsTokenSets.RS_COMMENTS;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.version.LanguageVersion;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.impl.RsElementTypesFactory;

@ExtensionImpl
public class RustParserDefinition implements ParserDefinition {

    /**
     * Should be increased after any change of lexer rules
     */
    public static final int LEXER_VERSION = 6;

    /**
     * Should be increased after any change of parser rules
     */
    public static final int PARSER_VERSION = LEXER_VERSION + 51;

    @Nonnull
    @Override
    public PsiFile createFile(@Nonnull FileViewProvider viewProvider) {
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

    @Nonnull
    @Override
    public SpaceRequirements spaceExistenceTypeBetweenTokens(@Nonnull ASTNode left, @Nonnull ASTNode right) {
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

    @Nonnull
    @Override
    public IFileElementType getFileNodeType() {
        return RsFileStub.Type;
    }

    @Nonnull
    @Override
    public TokenSet getStringLiteralElements(@Nonnull consulo.language.version.LanguageVersion version) {
        return RS_ALL_STRING_LITERALS;
    }

    @Nonnull
    @Override
    public TokenSet getWhitespaceTokens(@Nonnull consulo.language.version.LanguageVersion version) {
        return TokenSet.create(TokenType.WHITE_SPACE);
    }

    @Nonnull
    @Override
    public TokenSet getCommentTokens(@Nonnull consulo.language.version.LanguageVersion version) {
        return RS_COMMENTS;
    }

    @Nonnull
    @Override
    public PsiElement createElement(@Nonnull ASTNode node) {
        return org.rust.lang.core.psi.impl.RsElementTypesFactory.createElement(node);
    }

    @Nonnull
    @Override
    public Lexer createLexer(@Nonnull consulo.language.version.LanguageVersion version) {
        return new RsLexer();
    }

    @Nonnull
    @Override
    public PsiParser createParser(@Nonnull consulo.language.version.LanguageVersion version) {
        return new RustParser();
    }

    @Nonnull
    @Override
    public consulo.language.Language getLanguage() {
        return org.rust.lang.RsLanguage.INSTANCE;
    }
}
