/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.completion.ml.MLRankingIgnorable;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.impl.MacroCallNode;
import com.intellij.codeInsight.template.macro.CompleteMacro;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.template.postfix.MatchPostfixTemplate;
import org.rust.ide.utils.template.RsTemplateBuilder;
import org.rust.ide.utils.template.EditorExtUtil;
import org.rust.lang.core.RsPsiPatternUtil;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.completion.RsLookupElementProperties.KeywordKind;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.openapiext.EditorExt;
import org.rust.openapiext.SmartPointerExtUtil;

import java.util.Arrays;
import java.util.List;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.patterns.StandardPatterns.or;
import static org.rust.lang.core.psi.RsElementTypes.*;
import static org.rust.lang.core.psi.RsTokenType.RS_COMMENTS;
import static org.rust.lang.core.psi.RsTokenType.tokenSetOf;

/**
 * Completes Rust keywords
 *
 */
public class RsKeywordCompletionContributor extends CompletionContributor implements DumbAware {

    public static final List<String> CONDITION_KEYWORDS = Arrays.asList("if", "match");

    public static final TokenSet RS_VIS_ALLOWED_TOKENS = TokenSet.orSet(
        tokenSetOf(
            PUB,
            LPAREN,
            SUPER,
            CRATE,
            SELF,
            COLONCOLON,
            IN,
            PATH,
            IDENTIFIER,
            RPAREN,
            TokenType.WHITE_SPACE,
            TokenType.ERROR_ELEMENT
        ),
        RS_COMMENTS
    );

    private final PsiElementPattern.Capture<PsiElement> afterUseItemWithoutGroupingWithoutWildcard = RsPsiPatternUtil.withPrevSiblingSkipping(psiElement(),
        RsPsiPattern.whitespace,
            psiElement(RsUseItem.class).with(new PatternCondition<RsUseItem>("afterUseItemWithoutGroupingWithoutWildcard") {
                @Override
                public boolean accepts(@NotNull RsUseItem t, ProcessingContext context) {
                    RsUseSpeck speck = t.getUseSpeck();
                    if (speck == null) return true;
                    return speck.getMul() == null && speck.getUseGroup() == null;
                }
            }));

    private final PsiElementPattern.Capture<PsiElement> insideUseGroupAfterIdentifierWithoutWildcard = psiElement().withAncestor(2,
        RsPsiPatternUtil.withPrevSiblingSkipping(psiElement(),
            StandardPatterns.or(RsPsiPattern.whitespace, psiElement(PsiErrorElement.class)),
                psiElement(RsUseSpeck.class).with(new PatternCondition<RsUseSpeck>("insideUseGroupAfterIdentifierWithoutWildcard") {
                    @Override
                    public boolean accepts(@NotNull RsUseSpeck t, ProcessingContext context) {
                        return t.getMul() == null;
                    }
                })));

    public RsKeywordCompletionContributor() {
        extend(CompletionType.BASIC, declarationPattern(),
            new RsKeywordCompletionProvider("const", "async", "enum", "extern", "fn", "impl", "mod", "static", "struct", "trait", "type", "union", "unsafe", "use"));
        extend(CompletionType.BASIC, afterVisDeclarationPattern(),
            new RsKeywordCompletionProvider("const", "async", "enum", "extern", "fn", "mod", "static", "struct", "trait", "type", "union", "unsafe", "use"));
        extend(CompletionType.BASIC, externDeclarationPattern(),
            new RsKeywordCompletionProvider("crate", "fn"));
        extend(CompletionType.BASIC, unsafeDeclarationPattern(),
            new RsKeywordCompletionProvider("fn", "impl", "trait", "extern"));
        extend(CompletionType.BASIC, newCodeStatementPattern(),
            new RsKeywordCompletionProvider("return", "let"));
        extend(CompletionType.BASIC, letPattern(),
            new RsKeywordCompletionProvider("mut"));
        extend(CompletionType.BASIC, loopFlowCommandPattern(),
            new RsKeywordCompletionProvider("break", "continue"));
        extend(CompletionType.BASIC, wherePattern(),
            new RsKeywordCompletionProvider("where"));
        extend(CompletionType.BASIC, constParameterBeginningPattern(),
            new RsKeywordCompletionProvider("const"));
        extend(CompletionType.BASIC, inherentImplDeclarationPattern(),
            new RsKeywordCompletionProvider("async"));
        extend(CompletionType.BASIC, structLiteralPathPattern(),
            new RsKeywordCompletionProvider("async"));
        extend(CompletionType.BASIC, traitOrImplDeclarationPattern(),
            new RsKeywordCompletionProvider("const", "fn", "type", "unsafe"));
        extend(CompletionType.BASIC, unsafeTraitOrImplDeclarationPattern(),
            new RsKeywordCompletionProvider("fn"));
        extend(CompletionType.BASIC, asyncDeclarationPattern(),
            new RsKeywordCompletionProvider("fn"));
        extend(CompletionType.BASIC, afterVisInherentImplDeclarationPattern(),
            new RsKeywordCompletionProvider("const", "fn", "type", "unsafe"));
        extend(CompletionType.BASIC, asPattern(),
            new RsKeywordCompletionProvider("as"));

        extend(CompletionType.BASIC, ifElsePattern(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                LookupElementBuilder elseBuilder = elseLookupElement();
                LookupElementBuilder elseIfBuilder = conditionLookupElement("else if");
                // `else` is more common than `else if`
                result.addElement(LookupElements.toKeywordElement(elseBuilder, KeywordKind.ELSE_BRANCH));
                result.addElement(LookupElements.toKeywordElement(elseIfBuilder));
            }
        });

        extend(CompletionType.BASIC, afterLetDecl(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                LookupElementBuilder elseBuilder = elseLookupElement();
                result.addElement(LookupElements.toKeywordElement(elseBuilder));
            }
        });

        extend(CompletionType.BASIC, pathExpressionPattern(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                for (String keyword : CONDITION_KEYWORDS) {
                    result.addElement(LookupElements.toKeywordElement(conditionLookupElement(keyword)));
                }
            }
        });

        extend(CompletionType.BASIC, pathExpressionPattern(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                result.addElement(LookupElements.toKeywordElement(lambdaLookupElement("async")));
            }
        });

        extendWithFnTypeCompletion();

        extend(CompletionType.BASIC, afterIfOrWhilePattern(), new RsKeywordCompletionProvider("let"));
        extend(CompletionType.BASIC, afterImplTraitPattern(), new RsKeywordCompletionProvider("for"));
    }

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        super.fillCompletionVariants(parameters, RsCompletionContributor.withRustSorter(parameters, result));
    }

    private LookupElementBuilder conditionLookupElement(String keyword) {
        return LookupElementBuilder
            .create(keyword)
            .bold()
            .withTailText(" {...}")
            .withInsertHandler((context, item) -> conditionLookupElementHandleInsert(context, keyword));
    }

    private void conditionLookupElementHandleInsert(InsertionContext context, String keyword) {
        RsExpr element0;
        switch (keyword) {
            case "if":
            case "else if":
                element0 = LookupElements.getElementOfType(context, RsIfExpr.class);
                break;
            case "match":
                element0 = LookupElements.getElementOfType(context, RsMatchExpr.class);
                break;
            default:
                element0 = null;
                break;
        }
        if (element0 == null) return;
        SmartPsiElementPointer<RsExpr> elementPointer = SmartPointerExtUtil.createSmartPointer(element0);

        PsiElement parent = element0.getParent();
        boolean isLetDecl = parent instanceof RsLetDecl;
        String semicolon = isLetDecl && !LookupElements.nextCharIs(context, ';') ? ";" : "";
        // `f` is condition expr which will be replaced by template builder
        context.getDocument().insertString(context.getSelectionEndOffset(), " f {  }" + semicolon);
        PsiDocumentManager.getInstance(context.getProject()).commitDocument(context.getDocument());

        RsExpr element1 = elementPointer.getElement();
        if (element1 == null) return;
        RsExpr expr;
        if (element1 instanceof RsIfExpr) {
            RsCondition condition = ((RsIfExpr) element1).getCondition();
            expr = condition != null ? condition.getExpr() : null;
        } else if (element1 instanceof RsMatchExpr) {
            expr = ((RsMatchExpr) element1).getExpr();
        } else {
            expr = null;
        }
        if (expr == null) return;
        RsTemplateBuilder tpl = EditorExtUtil.newTemplateBuilder(context.getEditor(), element1);
        tpl.replaceElement(expr, new MacroCallNode(new CompleteMacro()));
        tpl.runInline(() -> {
            RsExpr element2 = elementPointer.getElement();
            if (element2 == null) return;
            EditorExt.moveCaretToOffset(context.getEditor(), element2, element2.getTextRange().getEndOffset() - " }".length());
            if (element2 instanceof RsMatchExpr && !DumbService.isDumb(element2.getProject())) {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    MatchPostfixTemplate.fillMatchArms((RsMatchExpr) element2, context.getEditor());
                });
            }
        });
    }

    private LookupElementBuilder lambdaLookupElement(String lookupString) {
        return LookupElementBuilder
            .create(lookupString)
            .bold()
            .withTailText(" {...}")
            .withInsertHandler((context, item) -> {
                PsiElement found = context.getFile().findElementAt(context.getTailOffset() - 1);
                RsLetDecl letDecl = found != null ? RsElementUtil.ancestorStrict(found, RsLetDecl.class) : null;
                boolean isLetExpr = letDecl != null && letDecl.getExpr() != null
                    && lookupString.equals(letDecl.getExpr().getText());
                boolean hasSemicolon = LookupElements.nextCharIs(context, ';');

                String tail = " {  }";
                if (isLetExpr && !hasSemicolon) tail += ";";
                context.getDocument().insertString(context.getSelectionEndOffset(), tail);
                EditorModificationUtil.moveCaretRelatively(context.getEditor(), 3);
            });
    }

    private PsiElementPattern.Capture<PsiElement> afterVisDeclarationPattern() {
        return RsPsiPattern.baseDeclarationPattern().and(afterVis());
    }

    private PsiElementPattern.Capture<PsiElement> externDeclarationPattern() {
        return RsPsiPattern.baseDeclarationPattern().and(statementBeginningPattern("extern"));
    }

    private PsiElementPattern.Capture<PsiElement> unsafeDeclarationPattern() {
        return RsPsiPattern.baseDeclarationPattern().and(statementBeginningPattern("unsafe"));
    }

    private PsiElementPattern.Capture<PsiElement> newCodeStatementPattern() {
        return baseCodeStatementPattern().and(statementBeginningPattern());
    }

    private PsiElementPattern.Capture<PsiElement> letPattern() {
        return baseCodeStatementPattern().and(statementBeginningPattern("let"));
    }

    @SuppressWarnings("unchecked")
    private PsiElementPattern.Capture<PsiElement> loopFlowCommandPattern() {
        return RsPsiPattern.inAnyLoop.and(
            StandardPatterns.or(newCodeStatementPattern(), pathExpressionPattern())
        );
    }

    private PsiElementPattern.Capture<PsiElement> baseCodeStatementPattern() {
        return psiElement()
            .inside(psiElement(RsFunction.class))
            .andNot(psiElement().withParent(RsModItem.class))
            .andNot(psiElement().withSuperParent(2, RsStructLiteralBody.class))
            .andNot(psiElement().withSuperParent(3, RsPatStruct.class));
    }

    private PsiElementPattern.Capture<PsiElement> statementBeginningPattern(String... startWords) {
        return psiElement(IDENTIFIER).and(RsPsiPattern.onStatementBeginning(startWords));
    }

    private PsiElementPattern.Capture<PsiElement> ifElsePattern() {
        ElementPattern<PsiElement> braceAfterIf = psiElement(RBRACE).withSuperParent(2, psiElement(IF_EXPR));
        return psiElement().afterLeafSkipping(RsPsiPattern.whitespace, braceAfterIf);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private PsiElementPattern.Capture<PsiElement> asPattern() {
        return (PsiElementPattern.Capture<PsiElement>) (PsiElementPattern.Capture) StandardPatterns.or(
            afterExpr().andNot(psiElement().with(new PatternCondition<PsiElement>("isMacroCall") {
                @Override
                public boolean accepts(@NotNull PsiElement psi, ProcessingContext context) {
                    return PsiTreeUtil.getContextOfType(psi, RsMacroCall.class) != null;
                }
            })),
            afterUseItemWithoutGroupingWithoutWildcard,
            insideUseGroupAfterIdentifierWithoutWildcard
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private PsiElementPattern.Capture<PsiElement> afterLetDecl() {
        ElementPattern<PsiElement> withSemicolon = psiElement().withLastChildSkipping(RsPsiPattern.whitespace, psiElement(SEMICOLON));
        ElementPattern letPattern = psiElement(RsLetDecl.class).andNot(psiElement().and(withSemicolon));
        PsiElementPattern.Capture<PsiElement> parent = RsPsiPatternUtil.withPrevSiblingSkipping(psiElement(), RsPsiPattern.whitespace, letPattern);
        return psiElement().andOr(
            psiElement().withSuperParent(2, parent),   // let _ = ... /*caret*/
            psiElement().withSuperParent(3, parent));   // let _ = ... /*caret*/;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private PsiElementPattern.Capture<PsiElement> wherePattern() {
        ElementPattern typeParameters = psiElement(RsTypeParameterList.class);

        ElementPattern function = psiElement(RsFunction.class)
            .withLastChildSkipping(RsPsiPattern.error, or(psiElement(RsValueParameterList.class), psiElement(RsRetType.class)))
            .andOr(
                psiElement().withChild(psiElement(RsTypeParameterList.class)),
                psiElement().withParent(RsMembers.class)
            );

        ElementPattern struct = psiElement(RsStructItem.class)
            .withChild(typeParameters)
            .withLastChildSkipping(RsPsiPattern.error, or(typeParameters, psiElement(RsTupleFields.class)));

        ElementPattern enumPattern = psiElement(RsEnumItem.class)
            .withLastChildSkipping(RsPsiPattern.error, typeParameters);

        ElementPattern typeAlias = psiElement(RsTypeAlias.class)
            .withLastChildSkipping(RsPsiPattern.error, typeParameters)
            .andNot(psiElement().withParent(RsMembers.class));

        ElementPattern trait = psiElement(RsTraitItem.class)
            .withLastChildSkipping(RsPsiPattern.error, or(psiElement(IDENTIFIER), typeParameters));

        ElementPattern impl = psiElement(RsImplItem.class)
            .withLastChildSkipping(RsPsiPattern.error, psiElement(RsTypeReference.class));

        return RsPsiPatternUtil.withPrevSiblingSkipping(psiElement(),
            RsPsiPattern.whitespace, or(function, struct, enumPattern, typeAlias, trait, impl));
    }

    private PsiElementPattern.Capture<PsiElement> pathExpressionPattern() {
        PsiElementPattern.Capture<RsPath> parent = psiElement(RsPath.class)
            .with(new PatternCondition<RsPath>("RsPath") {
                @Override
                public boolean accepts(@NotNull RsPath t, ProcessingContext context) {
                    return t.getPath() == null && t.getTypeQual() == null;
                }
            });

        return psiElement(IDENTIFIER)
            .withParent(parent)
            .withSuperParent(2, psiElement(RsPathExpr.class))
            .inside(psiElement(RsFunction.class));
    }

    private PsiElementPattern.Capture<PsiElement> pathTypePattern() {
        PsiElementPattern.Capture<RsPath> parent = psiElement(RsPath.class)
            .with(new PatternCondition<RsPath>("RsPath") {
                @Override
                public boolean accepts(@NotNull RsPath path, ProcessingContext context) {
                    PsiElement identifier = path.getIdentifier();
                    return path.getFirstChild() == identifier && path.getLastChild() == identifier;
                }
            });

        return psiElement(IDENTIFIER)
            .withParent(parent)
            .withSuperParent(2, psiElement(RsPathType.class));
    }

    private PsiElementPattern.Capture<PsiElement> constParameterBeginningPattern() {
        PsiElementPattern.Capture<RsTypeParameter> parent = psiElement(RsTypeParameter.class)
            .with(new PatternCondition<RsTypeParameter>("RsConstParameterBeginning") {
                @Override
                public boolean accepts(@NotNull RsTypeParameter t, ProcessingContext context) {
                    PsiElement leftSibling = null;
                    for (PsiElement sib = t.getPrevSibling(); sib != null; sib = sib.getPrevSibling()) {
                        if (!(sib instanceof PsiWhiteSpace)) {
                            leftSibling = sib;
                            break;
                        }
                    }
                    if (leftSibling != null && leftSibling.getNode().getElementType() != LT
                        && leftSibling.getNode().getElementType() != COMMA) {
                        return false;
                    }

                    PsiElement rightSibling = null;
                    for (PsiElement sib = t.getNextSibling(); sib != null; sib = sib.getNextSibling()) {
                        if (sib instanceof RsElement) {
                            rightSibling = sib;
                            break;
                        }
                    }
                    if (rightSibling instanceof RsTypeParameter || rightSibling instanceof RsLifetimeParameter) {
                        return false;
                    }

                    return true;
                }
            });

        return psiElement(IDENTIFIER).withParent(parent);
    }

    private PsiElementPattern.Capture<PsiElement> traitOrImplDeclarationPattern() {
        return RsPsiPattern.baseTraitOrImplDeclaration().and(statementBeginningPattern());
    }

    private PsiElementPattern.Capture<PsiElement> inherentImplDeclarationPattern() {
        return RsPsiPattern.baseInherentImplDeclarationPattern().and(statementBeginningPattern());
    }

    private PsiElementPattern.Capture<PsiElement> structLiteralPathPattern() {
        return RsPsiPattern.getSimplePathPattern().withSuperParent(2, psiElement(RsStructLiteral.class));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private PsiElementPattern.Capture<PsiElement> asyncDeclarationPattern() {
        return psiElement()
            .andOr(RsPsiPattern.baseDeclarationPattern(), RsPsiPattern.baseInherentImplDeclarationPattern())
            .and(statementBeginningPattern("async"));
    }

    private PsiElementPattern.Capture<PsiElement> unsafeTraitOrImplDeclarationPattern() {
        return RsPsiPattern.baseTraitOrImplDeclaration().and(statementBeginningPattern("unsafe"));
    }

    private PsiElementPattern.Capture<PsiElement> afterVisInherentImplDeclarationPattern() {
        return RsPsiPattern.baseInherentImplDeclarationPattern().and(afterVis());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private PsiElementPattern.Capture<PsiElement> afterIfOrWhilePattern() {
        return psiElement().andOr(afterIfPattern(), afterWhilePattern());
    }

    private PsiElementPattern.Capture<PsiElement> afterIfPattern() {
        return psiElement().afterLeaf(psiElement(IF).withParent(psiElement(IF_EXPR)));
    }

    private PsiElementPattern.Capture<PsiElement> afterWhilePattern() {
        return psiElement().afterLeaf(psiElement(WHILE).withParent(psiElement(WHILE_EXPR)));
    }

    private PsiElementPattern.Capture<PsiElement> afterImplTraitPattern() {
        PsiElementPattern.Capture<RsImplItem> impl = psiElement(RsImplItem.class)
            .withLastChildSkipping(RsPsiPattern.error, psiElement(RsPathType.class).with(new PatternCondition<RsPathType>("isTrait") {
                @Override
                public boolean accepts(@NotNull RsPathType it, ProcessingContext context) {
                    if (DumbService.isDumb(it.getProject())) {
                        // Considering that `impl Struct for` (where `Struct` is a struct, not a trait) is invalid code,
                        // it will not be written often
                        // So we can assume that a trait is specified
                        return true;
                    } else {
                        RsPath path = it.getPath();
                        if (path == null) return false;
                        PsiElement resolved = path.getReference() != null ? path.getReference().resolve() : null;
                        return resolved instanceof RsTraitItem;
                    }
                }
            }));

        return RsPsiPatternUtil.withPrevSiblingSkipping(psiElement(),
            RsPsiPattern.whitespace, impl);
    }

    // TODO(parser recovery?): it would be really nice to just say something like element.prevSibling is RsVis
    private PsiElementPattern.Capture<PsiElement> afterVis() {
        return psiElement().with(new PatternCondition<PsiElement>("afterVis") {
            @Override
            public boolean accepts(@NotNull PsiElement item, ProcessingContext context) {
                PsiElement current = item;
                PsiElement last = null;
                while (current != null) {
                    IElementType type = current.getNode().getElementType();
                    if (!(current instanceof RsPath || RS_VIS_ALLOWED_TOKENS.contains(type))) {
                        break;
                    }
                    if (!(current instanceof PsiWhiteSpace) && !(current instanceof PsiComment)) {
                        last = current;
                    }
                    current = current.getPrevSibling();
                }
                return last != null && last.getNode().getElementType() == PUB;
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private PsiElementPattern.Capture<PsiElement> afterExpr() {
        return RsPsiPatternUtil.withPrevLeafSkipping(psiElement(),
            (ElementPattern) StandardPatterns.or(psiElement(PsiErrorElement.class), psiElement(PsiWhiteSpace.class)),
            psiElement(PsiElement.class).with(new PatternCondition<PsiElement>("previousLeafIsInsideExpr") {
                @Override
                public boolean accepts(@NotNull PsiElement leaf, ProcessingContext context) {
                    int leafEndOffset = leaf.getTextRange().getEndOffset();
                    PsiElement current = leaf;
                    while (current != null) {
                        if (current instanceof RsExpr && current.getTextRange().getEndOffset() == leafEndOffset) {
                            return true;
                        }
                        if (current instanceof PsiFile) break;
                        current = current.getContext();
                    }
                    return false;
                }
            })
        );
    }

    private LookupElementBuilder elseLookupElement() {
        return LookupElementBuilder
            .create("else")
            .bold()
            .withTailText(" {...}")
            .withInsertHandler((ctx, item) -> {
                ctx.getDocument().insertString(ctx.getSelectionEndOffset(), " {  }");
                EditorModificationUtil.moveCaretRelatively(ctx.getEditor(), 3);
            });
    }

    private void extendWithFnTypeCompletion() {
        extend(CompletionType.BASIC, pathTypePattern(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                LookupElementBuilder lookup = LookupElementBuilder
                    .create("fn")
                    .bold()
                    .withTailText("()")
                    .withInsertHandler((ctx, item) -> {
                        ctx.getDocument().insertString(ctx.getSelectionEndOffset(), "()");
                        EditorModificationUtil.moveCaretRelatively(ctx.getEditor(), 1);
                    });
                LookupElement rsLookup = LookupElements.toRsLookupElement(lookup, new RsLookupElementProperties());

                @SuppressWarnings("UnstableApiUsage")
                LookupElement wrapped = MLRankingIgnorable.wrap(rsLookup);
                result.addElement(wrapped);
            }
        });
    }

    private PsiElementPattern.Capture<PsiElement> declarationPattern() {
        return RsPsiPattern.declarationPattern();
    }
}
