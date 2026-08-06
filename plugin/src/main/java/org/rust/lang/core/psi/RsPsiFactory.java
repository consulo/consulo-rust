/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.StubBasedPsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.ElementManipulator;
import consulo.language.psi.LiteralTextEscaper;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.ContributedReferenceHost;
import consulo.language.psi.SyntaxTraverser;
import consulo.language.psi.PsiParserFacade;
import consulo.util.lang.LocalTimeCounter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.presentation.TypeRendering;
import org.rust.ide.utils.checkMatch.Pattern;
import org.rust.lang.RsFileType;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.macros.MacroExpansionContext;
import org.rust.lang.core.parser.RustParserUtil.PathParsingMode;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.infer.FoldUtil;
import org.rust.lang.core.types.ty.Mutability;
import org.rust.lang.core.types.ty.Ty;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RsPsiFactory {
    @Nonnull
    private final Project project;
    private final boolean markGenerated;
    private final boolean eventSystemEnabled;

    public RsPsiFactory(@Nonnull Project project) {
        this(project, true, false);
    }

    public RsPsiFactory(@Nonnull Project project, boolean markGenerated) {
        this(project, markGenerated, false);
    }

    public RsPsiFactory(@Nonnull Project project, boolean markGenerated, boolean eventSystemEnabled) {
        this.project = project;
        this.markGenerated = markGenerated;
        this.eventSystemEnabled = eventSystemEnabled;
    }

    @Nonnull
    public RsFile createFile(@Nonnull CharSequence text) {
        return (RsFile) createPsiFile(text);
    }

    @Nonnull
    public PsiFile createPsiFile(@Nonnull CharSequence text) {
        return PsiFileFactory.getInstance(project)
            .createFileFromText(
                "DUMMY.rs",
                RsFileType.INSTANCE,
                text,
                LocalTimeCounter.currentTime(),
                eventSystemEnabled,
                markGenerated
            );
    }

    @Nullable
    public RsMacroBody createMacroBody(@Nonnull String text) {
        return createFromText("macro_rules! m " + text, RsMacroBody.class);
    }

    @Nonnull
    public RsMacroCall createMacroCall(
        @Nonnull MacroExpansionContext context,
        @Nonnull MacroBraces braces,
        @Nonnull String macroName,
        @Nonnull String... arguments
    ) {
        return createMacroCall(context, braces, macroName, String.join(", ", arguments));
    }

    @Nonnull
    public RsMacroCall createMacroCall(
        @Nonnull MacroExpansionContext context,
        @Nonnull MacroBraces braces,
        @Nonnull String macroName,
        @Nonnull String argument
    ) {
        boolean appendSemicolon = (context == MacroExpansionContext.ITEM || context == MacroExpansionContext.STMT)
            && braces.getNeedsSemicolon();
        String semicolon = appendSemicolon ? ";" : "";
        String code = context.prepareExpandedTextForParsing(
            macroName + "!" + braces.wrap(argument) + semicolon
        ).toString();
        RsMacroCall result = createFromText(code, RsMacroCall.class);
        if (result == null) throw new IllegalStateException("Failed to create macro call");
        return result;
    }

    @Nonnull
    public RsFormatMacroArg createFormatMacroArg(@Nonnull String argument) {
        RsFormatMacroArg result = createFromText("print!(" + argument + ")", RsFormatMacroArg.class);
        if (result == null) throw new IllegalStateException("Failed to create format macro argument");
        return result;
    }

    @Nonnull
    public RsSelfParameter createSelf(boolean mutable) {
        RsFunction fn = createFromText("fn main(" + (mutable ? "mut " : "") + "self){}", RsFunction.class);
        if (fn == null || fn.getSelfParameter() == null) throw new IllegalStateException("Failed to create self element");
        return fn.getSelfParameter();
    }

    @Nonnull
    public RsSelfParameter createSelfReference(boolean mutable) {
        RsFunction fn = createFromText("fn main(&" + (mutable ? "mut " : "") + "self){}", RsFunction.class);
        if (fn == null || fn.getSelfParameter() == null) throw new IllegalStateException("Failed to create self element");
        return fn.getSelfParameter();
    }

    @Nonnull
    public RsSelfParameter createSelfWithType(@Nonnull String text) {
        RsFunction fn = createFromText("fn main(self: " + text + "){}", RsFunction.class);
        if (fn == null || fn.getSelfParameter() == null) throw new IllegalStateException("Failed to create self element");
        return fn.getSelfParameter();
    }

    @Nonnull
    public PsiElement createIdentifier(@Nonnull String text) {
        RsModDeclItem modDecl = createFromText("mod " + RsRawIdentifiers.escapeIdentifierIfNeeded(text) + ";", RsModDeclItem.class);
        if (modDecl == null || modDecl.getIdentifier() == null) {
            throw new IllegalStateException("Failed to create identifier: `" + text + "`");
        }
        return modDecl.getIdentifier();
    }

    @Nonnull
    public PsiElement createQuoteIdentifier(@Nonnull String text) {
        RsLifetimeParameter param = createFromText("fn foo<" + text + ">(_: &" + text + " u8) {}", RsLifetimeParameter.class);
        if (param == null || param.getQuoteIdentifier() == null) {
            throw new IllegalStateException("Failed to create quote identifier: `" + text + "`");
        }
        return param.getQuoteIdentifier();
    }

    @Nonnull
    public PsiElement createMetavarIdentifier(@Nonnull String text) {
        RsMetaVarIdentifier result = createFromText("macro m { ($ " + text + ") => () }", RsMetaVarIdentifier.class);
        if (result == null) throw new IllegalStateException("Failed to create metavar identifier: `" + text + "`");
        return result;
    }

    @Nonnull
    public RsExpr createExpression(@Nonnull String text) {
        RsExpr result = tryCreateExpression(text);
        if (result == null) throw new IllegalStateException("Failed to create expression from text: `" + text + "`");
        return result;
    }

    @Nullable
    public RsExpr tryCreateExpression(@Nonnull CharSequence text) {
        return createFromText("fn main() { let _ = " + text + "; }", RsExpr.class);
    }

    @Nullable
    public RsExprStmt tryCreateExprStmtWithSemicolon(@Nonnull CharSequence text) {
        RsExprStmt stmt = createFromText("fn main() { " + text + "; }", RsExprStmt.class);
        return stmt != null && stmt.getTextLength() == text.length() + 1 ? stmt : null;
    }

    @Nullable
    public RsExprStmt tryCreateExprStmtWithoutSemicolon(@Nonnull CharSequence text) {
        RsExprStmt stmt = createFromText("fn main() { " + text + " }", RsExprStmt.class);
        return stmt != null && stmt.getTextLength() == text.length() ? stmt : null;
    }

    @Nonnull
    public RsTryExpr createTryExpression(@Nonnull RsExpr expr) {
        RsTryExpr newElement = createExpressionOfType("a?", RsTryExpr.class);
        newElement.getExpr().replace(expr);
        return newElement;
    }

    @Nonnull
    public RsIfExpr createIfExpression(@Nonnull RsExpr condition, @Nonnull RsExpr thenBranch) {
        RsIfExpr result = createExpressionOfType("if " + condition.getText() + " { () }", RsIfExpr.class);
        RsBlock block = result.getBlock();
        if (block == null) throw new IllegalStateException("Failed to create if expression");
        if (thenBranch instanceof RsBlockExpr) {
            block.replace(((RsBlockExpr) thenBranch).getBlock());
        } else {
            RsBlockUtil.getSyntaxTailStmt(block).getExpr().replace(thenBranch);
        }
        return result;
    }

    @Nonnull
    public RsIfExpr createIfElseExpression(@Nonnull RsExpr condition, @Nonnull RsBlock thenBlock, @Nonnull RsBlock elseBlock) {
        RsIfExpr result = createExpressionOfType("if " + condition.getText() + " { () } else { () }", RsIfExpr.class);
        result.getBlock().replace(thenBlock);
        result.getElseBranch().getBlock().replace(elseBlock);
        return result;
    }

    @Nonnull
    public RsBlockExpr createBlockExpr(@Nonnull CharSequence body) {
        return createExpressionOfType("{ " + body + " }", RsBlockExpr.class);
    }

    @Nonnull
    public RsElement createUnsafeBlockExprOrStmt(@Nonnull PsiElement body) {
        if (body instanceof RsExpr) {
            return createExpressionOfType("unsafe { " + body.getText() + " }", RsBlockExpr.class);
        } else if (body instanceof RsStmt) {
            RsExprStmt result = createFromText("fn f() { unsafe { " + body.getText() + " } }", RsExprStmt.class);
            if (result == null) throw new IllegalStateException("Failed to create unsafe block");
            return result;
        }
        throw new IllegalStateException("Unsupported element type: " + body);
    }

    @Nonnull
    public RsRetExpr createRetExpr(@Nonnull String expr) {
        return createExpressionOfType("return " + expr, RsRetExpr.class);
    }

    @Nullable
    public RsPath tryCreatePath(@Nonnull String text) {
        return tryCreatePath(text, PathParsingMode.TYPE);
    }

    @Nullable
    public RsPath tryCreatePath(@Nonnull String text, @Nonnull PathParsingMode ns) {
        RsPath path;
        switch (ns) {
            case TYPE:
                path = createFromText("fn foo(t: " + text + ") {}", RsPath.class);
                break;
            case VALUE:
                RsPathExpr pathExpr = createFromText("fn main() { " + text + "; }", RsPathExpr.class);
                path = pathExpr != null ? pathExpr.getPath() : null;
                break;
            default:
                throw new IllegalArgumentException(ns + " mode is not supported; use TYPE");
        }
        if (path == null || !path.getText().equals(text)) return null;
        return path;
    }

    @Nonnull
    public RsStructLiteral createStructLiteral(@Nonnull String name) {
        return createExpressionOfType(name + " { }", RsStructLiteral.class);
    }

    @Nonnull
    public RsStructLiteral createStructLiteral(@Nonnull String name, @Nonnull String bodyText) {
        return createExpressionOfType(name + " " + bodyText, RsStructLiteral.class);
    }

    @Nonnull
    public RsStructLiteral createStructLiteralWithFields(@Nonnull String name, @Nonnull String fields) {
        return createExpressionOfType(name + " " + fields, RsStructLiteral.class);
    }

    @Nonnull
    public RsStructLiteralField createStructLiteralField(@Nonnull String name, @Nonnull String value) {
        return createExpressionOfType("S { " + name + ": " + value + " }", RsStructLiteral.class)
            .getStructLiteralBody()
            .getStructLiteralFieldList()
            .get(0);
    }

    @Nonnull
    public RsStructLiteralField createStructLiteralField(@Nonnull String name, @Nullable RsExpr value) {
        RsStructLiteralField field = createExpressionOfType("S { " + name + ": () }", RsStructLiteral.class)
            .getStructLiteralBody()
            .getStructLiteralFieldList()
            .get(0);
        if (value != null && field.getExpr() != null) {
            field.getExpr().replace(value);
        }
        return field;
    }

    @Nonnull
    public RsNamedFieldDecl createStructNamedField(@Nonnull String text) {
        RsNamedFieldDecl result = createFromText("struct S { " + text + " }", RsNamedFieldDecl.class);
        if (result == null) throw new IllegalStateException("Failed to create block fields");
        return result;
    }

    @Nonnull
    public RsBlockFields createBlockFields(@Nonnull List<BlockField> fields) {
        String fieldsText = fields.stream()
            .map(f -> {
                String typeText = TypeRendering.renderInsertionSafe(f.type, true, false);
                return (f.addPub ? "pub " : " ") + f.name + ": " + typeText;
            })
            .collect(Collectors.joining(",\n"));
        RsBlockFields result = createFromText("struct S { " + fieldsText + " }", RsBlockFields.class);
        if (result == null) throw new IllegalStateException("Failed to create block fields");
        return result;
    }

    @Nonnull
    public RsTupleFields createTupleFields(@Nonnull List<TupleField> fields) {
        String fieldsText = fields.stream()
            .map(f -> {
                String typeText = TypeRendering.renderInsertionSafe(f.type, true, false);
                return (f.addPub ? "pub " : " ") + typeText;
            })
            .collect(Collectors.joining(", "));
        RsTupleFields result = createFromText("struct S(" + fieldsText + ")", RsTupleFields.class);
        if (result == null) throw new IllegalStateException("Failed to create tuple fields");
        return result;
    }

    @Nonnull
    public RsEnumVariant createEnumVariant(@Nonnull String text) {
        RsEnumItem e = createFromText("enum E { " + text + " }", RsEnumItem.class);
        if (e == null || RsEnumItemUtil.getVariants(e).size() != 1) {
            throw new IllegalStateException("Failed to create enum variant from text: `" + text + "`");
        }
        return RsEnumItemUtil.getVariants(e).get(0);
    }

    @Nonnull
    public RsStructItem createStruct(@Nonnull String text) {
        RsStructItem result = tryCreateStruct(text);
        if (result == null) throw new IllegalStateException("Failed to create struct from text: `" + text + "`");
        return result;
    }

    @Nullable
    public RsStructItem tryCreateStruct(@Nonnull String text) {
        return createFromText(text, RsStructItem.class);
    }

    @Nonnull
    public RsStmt createStatement(@Nonnull String text) {
        RsStmt result = createFromText("fn main() { " + text + " 92; }", RsStmt.class);
        if (result == null) throw new IllegalStateException("Failed to create statement from text: `" + text + "`");
        return result;
    }

    @Nonnull
    public RsLetDecl createLetDeclaration(
        @Nonnull String name,
        @Nullable RsExpr expr,
        boolean mutable,
        @Nullable RsTypeReference type
    ) {
        String mutStr = mutable ? "mut " : " ";
        String typeStr = type != null ? ": " + type.getText() : "";
        String exprStr = expr != null ? " = " + expr.getText() : "";
        return (RsLetDecl) createStatement("let " + mutStr + name + typeStr + exprStr + ";");
    }

    @Nonnull
    public RsLetDecl createLetDeclaration(@Nonnull String name, @Nullable RsExpr expr) {
        return createLetDeclaration(name, expr, false, null);
    }

    @Nonnull
    public RsTypeReference createType(@Nonnull CharSequence text) {
        RsTypeReference result = tryCreateType(text);
        if (result == null) throw new IllegalStateException("Failed to create type from text: `" + text + "`");
        return result;
    }

    @Nullable
    public RsTypeReference tryCreateType(@Nonnull CharSequence text) {
        return createFromText("fn main() { let a : " + text + "; }", RsTypeReference.class);
    }

    @Nonnull
    public PsiElement createMethodParam(@Nonnull String text) {
        RsFunction fnItem = createTraitMethodMember("fn foo(" + text + ");");
        if (fnItem.getSelfParameter() != null) return fnItem.getSelfParameter();
        if (!fnItem.getValueParameters().isEmpty()) return fnItem.getValueParameters().get(0);
        throw new IllegalStateException("Failed to create method param from text: `" + text + "`");
    }

    @Nonnull
    public RsRefLikeType createReferenceType(@Nonnull String innerTypeText, @Nonnull Mutability mutability) {
        return (RsRefLikeType) org.rust.lang.core.psi.ext.RsTypeReferenceUtil.skipParens(createType("&" + (mutability.isMut() ? "mut " : "") + innerTypeText));
    }

    @Nonnull
    public RsModDeclItem createModDeclItem(@Nonnull String modName) {
        RsModDeclItem result = tryCreateModDeclItem(modName);
        if (result == null) throw new IllegalStateException("Failed to create mod decl with name: `" + modName + "`");
        return result;
    }

    @Nullable
    public RsModDeclItem tryCreateModDeclItem(@Nonnull String modName) {
        return createFromText("mod " + RsRawIdentifiers.escapeIdentifierIfNeeded(modName) + ";", RsModDeclItem.class);
    }

    @Nonnull
    public RsUseItem createUseItem(@Nonnull String text, @Nonnull String visibility, @Nullable String alias) {
        String aliasText = alias != null && !alias.isEmpty() ? " as " + alias : "";
        RsUseItem result = createFromText(visibility + " use " + text + aliasText + ";", RsUseItem.class);
        if (result == null) throw new IllegalStateException("Failed to create use item from text: `" + text + "`");
        return result;
    }

    @Nonnull
    public RsUseItem createUseItem(@Nonnull String text) {
        return createUseItem(text, "", null);
    }

    @Nonnull
    public RsUseSpeck createUseSpeck(@Nonnull String text) {
        RsUseSpeck result = createFromText("use " + text + ";", RsUseSpeck.class);
        if (result == null) throw new IllegalStateException("Failed to create use speck from text: `" + text + "`");
        return result;
    }

    @Nonnull
    public RsExternCrateItem createExternCrateItem(@Nonnull String crateName) {
        RsExternCrateItem result = createFromText("extern crate " + crateName + ";", RsExternCrateItem.class);
        if (result == null) throw new IllegalStateException("Failed to create extern crate item: `" + crateName + "`");
        return result;
    }

    @Nonnull
    public RsModItem createModItem(@Nonnull String modName, @Nonnull String modText) {
        RsModItem result = createFromText("mod " + modName + " {\n" + modText + "\n}", RsModItem.class);
        if (result == null) {
            throw new IllegalStateException("Failed to create mod item with name: `" + modName + "` from text: `" + modText + "`");
        }
        return result;
    }

    @Nonnull
    public RsFunction createTraitMethodMember(@Nonnull String text) {
        RsFunction result = createFromText("trait Foo { " + text + " }", RsFunction.class);
        if (result == null) throw new IllegalStateException("Failed to create method member from text: `" + text + "`");
        return result;
    }

    @Nonnull
    public RsMembers createMembers(@Nonnull String text) {
        RsMembers result = createFromText("impl T for S {" + text + "}", RsMembers.class);
        if (result == null) throw new IllegalStateException("Failed to create members from text: `" + text + "`");
        return result;
    }

    @Nullable
    public RsImplItem tryCreateImplItem(@Nonnull String text) {
        return createFromText(text, RsImplItem.class);
    }

    @Nullable
    public RsTraitItem tryCreateTraitItem(@Nonnull String text) {
        return createFromText(text, RsTraitItem.class);
    }

    @Nonnull
    public RsImplItem createInherentImplItem(
        @Nonnull String name,
        @Nullable RsTypeParameterList typeParameterList,
        @Nullable RsWhereClause whereClause
    ) {
        return createImplTemplate(name, typeParameterList, whereClause);
    }

    @Nonnull
    public RsImplItem createInherentImplItem(@Nonnull String name) {
        return createInherentImplItem(name, null, null);
    }

    @Nonnull
    public RsImplItem createTraitImplItem(
        @Nonnull String type,
        @Nonnull String trait,
        @Nullable RsTypeParameterList typeParameterList,
        @Nullable RsWhereClause whereClause
    ) {
        return createImplTemplate(trait + " for " + type, typeParameterList, whereClause);
    }

    @Nonnull
    private RsImplItem createImplTemplate(
        @Nonnull String text,
        @Nullable RsTypeParameterList typeParameterList,
        @Nullable RsWhereClause whereClause
    ) {
        String whereText = whereClause != null ? whereClause.getText() : "";
        String typeParameterListText = typeParameterList != null ? typeParameterList.getText() : "";
        String typeArgumentListText = "";
        if (typeParameterList != null) {
            List<? extends RsElement> params = org.rust.lang.core.psi.ext.RsTypeParameterListUtil.getGenericParameters(typeParameterList);
            if (params != null && !params.isEmpty()) {
                typeArgumentListText = params.stream()
                    .map(p -> {
                        if (p instanceof RsLifetimeParameter) {
                            return ((RsLifetimeParameter) p).getQuoteIdentifier().getText();
                        } else {
                            return ((PsiNamedElement) p).getName();
                        }
                    })
                    .filter(s -> s != null)
                    .collect(Collectors.joining(", ", "<", ">"));
            }
        }
        RsImplItem result = createFromText(
            "impl" + typeParameterListText + " " + text + " " + typeArgumentListText + " " + whereText + " {  }",
            RsImplItem.class
        );
        if (result == null) throw new IllegalStateException("Failed to create impl item from text: `" + text + "`");
        return result;
    }

    @Nonnull
    public RsTypeParameterList createTypeParameterList(@Nonnull Iterable<String> params) {
        String text = String.join(", ", params);
        RsFunction fn = createFromText("fn foo<" + text + ">() {}", RsFunction.class);
        if (fn == null || fn.getTypeParameterList() == null) {
            throw new IllegalStateException("Failed to create type parameter list from text: `" + text + "`");
        }
        return fn.getTypeParameterList();
    }

    @Nonnull
    public RsTypeParameterList createTypeParameterList(@Nonnull String params) {
        RsFunction fn = createFromText("fn foo<" + params + ">() {}", RsFunction.class);
        if (fn == null || fn.getTypeParameterList() == null) {
            throw new IllegalStateException("Failed to create type parameters from text: `<" + params + ">`");
        }
        return fn.getTypeParameterList();
    }

    @Nonnull
    public RsOuterAttr createOuterAttr(@Nonnull String text) {
        RsOuterAttr result = createFromText("#[" + text + "] struct Dummy;", RsOuterAttr.class);
        if (result == null) throw new IllegalStateException("Failed to create outer attr from text: `" + text + "`");
        return result;
    }

    @Nonnull
    public RsInnerAttr createInnerAttr(@Nonnull String text) {
        RsInnerAttr result = createFromText("#![" + text + "]", RsInnerAttr.class);
        if (result == null) throw new IllegalStateException("Failed to create inner attr from text: `" + text + "`");
        return result;
    }

    @Nonnull
    public RsMetaItem createMetaItem(@Nonnull String text) {
        RsMetaItem result = createFromText("#[" + text + "] fn f(){}", RsMetaItem.class);
        if (result == null) throw new IllegalStateException("Failed to create meta item from text: `" + text + "`");
        return result;
    }

    @Nonnull
    public RsConstant createConstant(@Nonnull String name, @Nonnull RsExpr expr) {
        Ty exprType = org.rust.lang.core.psi.ext.RsExprUtil.getType(expr);
        String typeText = TypeRendering.renderInsertionSafe(exprType, true, false);
        RsConstant result = createFromText(
            "const " + name + ": " + typeText + " = " + expr.getText() + ";",
            RsConstant.class
        );
        if (result == null) throw new IllegalStateException("Failed to create constant " + name);
        return result;
    }

    @Nonnull
    public RsVis createPub() {
        RsVis result = createFromText("pub fn f() {}", RsVis.class);
        if (result == null) throw new IllegalStateException("Failed to create `pub` element");
        return result;
    }

    @Nonnull
    public RsVis createPubCrateRestricted() {
        RsVis result = createFromText("pub(crate) fn f() {}", RsVis.class);
        if (result == null) throw new IllegalStateException("Failed to create `pub(crate)` element");
        return result;
    }

    @Nonnull
    public PsiComment createBlockComment(@Nonnull String text) {
        return PsiParserFacade.getInstance(project).createBlockCommentFromText(RsLanguage.INSTANCE, text);
    }

    @Nonnull
    public PsiComment createLineComment(@Nonnull String text) {
        return PsiParserFacade.getInstance(project).createLineCommentFromText(RsFileType.INSTANCE, text);
    }

    @Nonnull
    public PsiElement createComma() {
        RsValueParameter param = createFromText("fn f(_ : (), )", RsValueParameter.class);
        assert param != null;
        return param.getNextSibling();
    }

    @Nonnull
    public PsiElement createSemicolon() {
        RsConstant c = createFromText("const C: () = ();", RsConstant.class);
        assert c != null && c.getSemicolon() != null;
        return c.getSemicolon();
    }

    @Nonnull
    public PsiElement createColon() {
        RsConstant c = createFromText("const C: () = ();", RsConstant.class);
        assert c != null && c.getColon() != null;
        return c.getColon();
    }

    @Nonnull
    public RsTypeParamBounds createTypeParamBounds(@Nonnull String bound) {
        RsTraitItem trait = createFromText("trait T: " + bound + " {}", RsTraitItem.class);
        assert trait != null && trait.getTypeParamBounds() != null;
        return trait.getTypeParamBounds();
    }

    @Nonnull
    public PsiElement createPlus() {
        RsTraitItem trait = createFromText("trait T: A + B {}", RsTraitItem.class);
        assert trait != null && trait.getTypeParamBounds() != null;
        java.util.List<RsPolybound> polybounds = trait.getTypeParamBounds().getPolyboundList();
        assert polybounds.size() >= 2;
        // The + is between the first polybound and the second
        consulo.language.psi.PsiElement next = polybounds.get(0).getNextSibling();
        while (next != null && next instanceof consulo.language.psi.PsiWhiteSpace) {
            next = next.getNextSibling();
        }
        assert next != null;
        return next;
    }

    @Nonnull
    public RsPolybound createPolybound(@Nonnull String bound) {
        RsTraitItem trait = createFromText("trait T: " + bound + " {}", RsTraitItem.class);
        assert trait != null && trait.getTypeParamBounds() != null;
        java.util.List<RsPolybound> polybounds = trait.getTypeParamBounds().getPolyboundList();
        assert !polybounds.isEmpty();
        return polybounds.get(0);
    }

    @Nonnull
    public PsiElement createColonColon() {
        RsPath path = tryCreatePath("std::mem");
        assert path != null && path.getColoncolon() != null;
        return path.getColoncolon();
    }

    @Nonnull
    public PsiElement createEq() {
        RsConstant c = createFromText("const C: () = ();", RsConstant.class);
        assert c != null && c.getEq() != null;
        return c.getEq();
    }

    @Nonnull
    public PsiElement createNewline() {
        return createWhitespace("\n");
    }

    @Nonnull
    public PsiElement createWhitespace(@Nonnull String ws) {
        return PsiParserFacade.getInstance(project).createWhiteSpaceFromText(ws);
    }

    @Nonnull
    public PsiElement createUnsafeKeyword() {
        RsFunction fn = createFromText("unsafe fn foo(){}", RsFunction.class);
        if (fn == null || fn.getUnsafe() == null) throw new IllegalStateException("Failed to create unsafe element");
        return fn.getUnsafe();
    }

    @Nonnull
    public RsFunction createFunction(@Nonnull String text) {
        RsFunction result = createFromText(text, RsFunction.class);
        if (result == null) throw new IllegalStateException("Failed to create function element: " + text);
        return result;
    }

    @Nonnull
    public RsRetType createRetType(@Nonnull String ty) {
        RsRetType result = createFromText("fn foo() -> " + ty + " {}", RsRetType.class);
        if (result == null) throw new IllegalStateException("Failed to create function return type: " + ty);
        return result;
    }

    @Nonnull
    public RsValueParameterList createSimpleValueParameterList(@Nonnull String name, @Nonnull RsTypeReference type) {
        RsFunction fn = createFromText("fn main(" + name + ": " + type.getText() + "){}", RsFunction.class);
        if (fn == null || fn.getValueParameterList() == null) {
            throw new IllegalStateException("Failed to create parameter element");
        }
        return fn.getValueParameterList();
    }

    @Nonnull
    public RsPatBinding createPatBinding(@Nonnull String name, boolean mutable, boolean ref) {
        String refStr = ref ? "ref " : "";
        String mutStr = mutable ? "mut " : "";
        RsLetDecl letDecl = (RsLetDecl) createStatement("let " + refStr + mutStr + name + " = 10;");
        RsPat pat = letDecl.getPat();
        if (pat == null || !(pat.getFirstChild() instanceof RsPatBinding)) {
            throw new IllegalStateException("Failed to create pat element");
        }
        return (RsPatBinding) pat.getFirstChild();
    }

    @Nonnull
    public RsPatBinding createPatBinding(@Nonnull String name) {
        return createPatBinding(name, false, false);
    }

    @Nonnull
    public RsPat createPat(@Nonnull String patText) {
        RsPat result = tryCreatePat(patText);
        if (result == null) throw new IllegalStateException("Failed to create pat element");
        return result;
    }

    @Nullable
    public RsPat tryCreatePat(@Nonnull String patText) {
        return ((RsLetDecl) createStatement("let " + patText + ";")).getPat();
    }

    @Nonnull
    public RsTraitType createDynTraitType(@Nonnull String pathText) {
        RsTraitType result = createFromText("type T = &dyn " + pathText + ";}", RsTraitType.class);
        if (result == null) throw new IllegalStateException("Failed to create trait type");
        return result;
    }

    @Nonnull
    public RsAssocTypeBinding createAssocTypeBinding(@Nonnull String name, @Nonnull String type) {
        RsAssocTypeBinding result = createFromText("type T = &dyn Trait<" + name + "=" + type + ">;", RsAssocTypeBinding.class);
        if (result == null) throw new IllegalStateException("Failed to create assoc type binding");
        return result;
    }

    @Nonnull
    public RsCastExpr createCastExpr(@Nonnull RsExpr expr, @Nonnull String typeText) {
        if (expr instanceof RsBinaryExpr) {
            return createExpressionOfType("(" + expr.getText() + ") as " + typeText, RsCastExpr.class);
        }
        return createExpressionOfType(expr.getText() + " as " + typeText, RsCastExpr.class);
    }

    @Nonnull
    public RsCallExpr createFunctionCall(@Nonnull String functionName, @Nonnull Iterable<? extends RsExpr> arguments) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (RsExpr arg : arguments) {
            if (!first) sb.append(", ");
            sb.append(arg.getText());
            first = false;
        }
        return createExpressionOfType(functionName + "(" + sb + ")", RsCallExpr.class);
    }

    @Nonnull
    public RsCallExpr createFunctionCall(@Nonnull String functionName, @Nonnull String arguments) {
        return createExpressionOfType(functionName + "(" + arguments + ")", RsCallExpr.class);
    }

    @Nonnull
    public RsDotExpr createNoArgsMethodCall(@Nonnull RsExpr expr, @Nonnull String methodNameText) {
        if (expr instanceof RsBinaryExpr || expr instanceof RsUnaryExpr || expr instanceof RsCastExpr) {
            return createExpressionOfType("(" + expr.getText() + ")." + methodNameText + "()", RsDotExpr.class);
        }
        return createExpressionOfType(expr.getText() + "." + methodNameText + "()", RsDotExpr.class);
    }

    @Nonnull
    public RsVisRestriction createVisRestriction(@Nonnull String pathText) {
        String inPrefix;
        switch (pathText) {
            case "crate":
            case "super":
            case "self":
                inPrefix = "";
                break;
            default:
                inPrefix = "in ";
                break;
        }
        RsFunction fn = createFromText("pub(" + inPrefix + pathText + ") fn foo() {}", RsFunction.class);
        if (fn == null || fn.getVis() == null || fn.getVis().getVisRestriction() == null) {
            throw new IllegalStateException("Failed to create vis restriction element");
        }
        return fn.getVis().getVisRestriction();
    }

    @Nullable
    public RsVis tryCreateVis(@Nonnull String text) {
        return createFromText(text + " fn foo() {}", RsVis.class);
    }

    @Nonnull
    public RsVis createVis(@Nonnull String text) {
        RsVis result = tryCreateVis(text);
        if (result == null) throw new IllegalStateException("Failed to create vis");
        return result;
    }

    @Nonnull
    public RsLabelDecl createLabelDeclaration(@Nonnull String name) {
        RsLabelDecl result = createFromText("fn main() { '" + name + ": while true {} }", RsLabelDecl.class);
        if (result == null) throw new IllegalStateException("Failed to create label decl");
        return result;
    }

    @Nonnull
    public RsLabel createLabel(@Nonnull String name) {
        RsLabel result = createFromText("fn main() { break '" + name + "; }", RsLabel.class);
        if (result == null) throw new IllegalStateException("Failed to create label");
        return result;
    }

    // --- Helper methods ---

    @SuppressWarnings("unchecked")
    @Nullable
    private <T extends RsElement> T createFromText(@Nonnull CharSequence code, @Nonnull Class<T> clazz) {
        PsiFile file = createFile(code);
        return RsPsiJavaUtil.descendantOfTypeStrict(file, clazz);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    private <E extends RsExpr> E createExpressionOfType(@Nonnull String text, @Nonnull Class<E> clazz) {
        RsExpr expr = createExpression(text);
        if (!clazz.isInstance(expr)) {
            throw new IllegalStateException("Failed to create " + clazz.getSimpleName() + " from `" + text + "`");
        }
        return clazz.cast(expr);
    }

    // --- Data classes ---
    public static final class BlockField {
        @Nonnull public final String name;
        @Nonnull public final Ty type;
        public final boolean addPub;

        public BlockField(@Nonnull String name, @Nonnull Ty type, boolean addPub) {
            this.name = name;
            this.type = type;
            this.addPub = addPub;
        }
    }

    public static final class TupleField {
        @Nonnull public final Ty type;
        public final boolean addPub;

        public TupleField(@Nonnull Ty type, boolean addPub) {
            this.type = type;
            this.addPub = addPub;
        }
    }

    // ===== Additional factory methods needed by IDE code =====

    @Nonnull
    public PsiElement createAsyncKeyword() {
        return createFromText("async fn f() {}", RsFunction.class).getFirstChild();
    }

    @Nonnull
    public RsCallExpr createAssocFunctionCall(@Nonnull String typeName, @Nonnull String methodName, @Nonnull String... args) {
        String argsStr = String.join(", ", args);
        return createFromText(typeName + "::" + methodName + "(" + argsStr + ");", RsCallExpr.class);
    }

    @Nonnull
    public RsCallExpr createAssocFunctionCall(@Nonnull String typeName, @Nonnull String methodName, @Nonnull List<RsExpr> args) {
        StringBuilder sb = new StringBuilder(typeName).append("::").append(methodName).append("(");
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(args.get(i).getText());
        }
        sb.append(")");
        return createFromText(sb + ";", RsCallExpr.class);
    }

    @Nonnull
    public RsTypeArgumentList createTypeArgumentList(@Nonnull List<String> types) {
        String typeArgs = "<" + String.join(", ", types) + ">";
        return createFromText("fn f() -> Foo" + typeArgs + " {}", RsTypeArgumentList.class);
    }

    @Nullable
    public RsMethodCall tryCreateMethodCall(@Nonnull RsExpr receiver, @Nonnull String methodName, @Nonnull List<RsExpr> args) {
        StringBuilder sb = new StringBuilder(receiver.getText()).append(".").append(methodName).append("(");
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(args.get(i).getText());
        }
        sb.append(")");
        try {
            return createFromText(sb + ";", RsMethodCall.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Nonnull
    public RsValueParameter createValueParameter(@Nonnull String name, @Nonnull RsTypeReference typeRef, boolean mutable, @Nullable RsLifetime lifetime) {
        String mutStr = mutable ? "mut " : "";
        return createFromText("fn f(" + mutStr + name + ": " + typeRef.getText() + ") {}", RsValueParameter.class);
    }

    @Nullable
    public RsValueParameter tryCreateValueParameter(@Nonnull String patText, @Nullable RsTypeReference typeRef, boolean mutable) {
        String mutStr = mutable ? "mut " : "";
        String typeStr = typeRef != null ? ": " + typeRef.getText() : ": ()";
        return createFromText("fn f(" + mutStr + patText + typeStr + ") {}", RsValueParameter.class);
    }

    @Nullable
    public RsValueParameter tryCreateValueParameter(@Nonnull String patText, @Nullable RsTypeReference typeRef) {
        return tryCreateValueParameter(patText, typeRef, false);
    }

    @Nonnull
    public RsPatRest createPatRest() {
        return createFromText("let (..) = x;", RsPatRest.class);
    }

    @Nonnull
    public RsMatchBody createMatchBody(@Nonnull List<?> arms) {
        StringBuilder sb = new StringBuilder("match x {");
        for (Object arm : arms) {
            sb.append(arm.toString()).append(",");
        }
        sb.append("}");
        return createFromText(sb.toString(), RsMatchBody.class);
    }

    @Nonnull
    public RsExpr createRefExpr(@Nonnull RsExpr expr, @Nonnull java.util.List<org.rust.lang.core.types.ty.Mutability> refs) {
        StringBuilder sb = new StringBuilder();
        for (org.rust.lang.core.types.ty.Mutability mut : refs) {
            sb.append(mut.isMut() ? "&mut " : "&");
        }
        sb.append(expr.getText());
        return createExpression(sb.toString());
    }

    @Nonnull
    public RsExpr createDerefExpr(@Nonnull RsExpr expr, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.append("*");
        sb.append(expr.getText());
        return createExpression(sb.toString());
    }

    @Nullable
    public RsValueArgumentList tryCreateValueArgumentList(@Nonnull List<RsExpr> args) {
        StringBuilder sb = new StringBuilder("f(");
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(args.get(i).getText());
        }
        sb.append(")");
        try {
            return createFromText(sb + ";", RsValueArgumentList.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Nonnull
    public PsiElement createIn() {
        RsForExpr forExpr = createFromText("for x in y {}", RsForExpr.class);
        if (forExpr == null) throw new IllegalStateException("Failed to create for expression");
        PsiElement in = forExpr.getIn();
        if (in == null) throw new IllegalStateException("Failed to find 'in' keyword");
        return in;
    }

    @Nonnull
    public RsExpr createBox(@Nonnull String text) {
        return createExpression("Box::new(" + text + ")");
    }

    @Nonnull
    public PsiElement createDotDotEq() {
        RsLetDecl letDecl = createFromText("let _ = 0..=1;", RsLetDecl.class);
        if (letDecl == null) throw new IllegalStateException("Failed to create let declaration");
        RsExpr expr = letDecl.getExpr();
        if (expr == null) throw new IllegalStateException("Failed to get expression");
        // Find the ..= operator token within the range expression
        PsiElement child = expr.getFirstChild();
        while (child != null) {
            if (child.getText().equals("..=")) return child;
            child = child.getNextSibling();
        }
        throw new IllegalStateException("Failed to find ..= operator");
    }

    @Nonnull
    public RsExpr createLoop(@Nonnull String label, @Nonnull String body) {
        String labelStr = label.isEmpty() ? "" : label + ": ";
        return createExpression(labelStr + "loop { " + body + " }");
    }

    @Nonnull
    public RsLambdaExpr createLambda(@Nonnull String text) {
        return createFromText("let _ = " + text + ";", RsLambdaExpr.class);
    }

    @Nonnull
    public RsPatTupleStruct createPatTupleStruct(@Nonnull RsStructItem struct, @Nonnull String name) {
        return createFromText("let " + name + "(..) = x;", RsPatTupleStruct.class);
    }

    @Nonnull
    public RsPatTupleStruct createPatTupleStruct(@Nonnull String name, @Nonnull java.util.List<? extends RsPat> pats) {
        StringBuilder sb = new StringBuilder();
        sb.append("let ").append(name).append("(");
        for (int i = 0; i < pats.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(pats.get(i).getText());
        }
        sb.append(") = x;");
        return createFromText(sb.toString(), RsPatTupleStruct.class);
    }

    @Nonnull
    public RsPatStruct createPatStruct(@Nonnull RsStructItem struct, @Nonnull String name) {
        return createFromText("let " + name + " { .. } = x;", RsPatStruct.class);
    }

    @Nonnull
    public RsPatStruct createPatStruct(@Nonnull String name, @Nonnull java.util.List<? extends RsPatField> fields, @org.jetbrains.annotations.Nullable RsPatRest patRest) {
        StringBuilder sb = new StringBuilder();
        sb.append("let ").append(name).append(" { ");
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(fields.get(i).getText());
        }
        if (patRest != null) {
            if (!fields.isEmpty()) sb.append(", ");
            sb.append("..");
        }
        sb.append(" } = x;");
        return createFromText(sb.toString(), RsPatStruct.class);
    }

    @Nonnull
    public RsPatTup createPatTuple(int size) {
        StringBuilder sb = new StringBuilder("let (");
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(", ");
            sb.append("_");
        }
        sb.append(") = x;");
        return createFromText(sb.toString(), RsPatTup.class);
    }

    @Nonnull
    public RsPatField createPatField(@Nonnull String fieldName) {
        return createFromText("let Foo { " + fieldName + " } = x;", RsPatField.class);
    }

    @Nonnull
    public RsPatFieldFull createPatFieldFull(@Nonnull String fieldName, @Nonnull String patText) {
        return createFromText("let Foo { " + fieldName + ": " + patText + " } = x;", RsPatFieldFull.class);
    }

    @Nonnull
    public RsWhereClause createWhereClause(@Nonnull List<RsLifetimeParameter> lifetimeParams, @Nonnull List<RsTypeParameter> typeParams) {
        StringBuilder sb = new StringBuilder("where ");
        boolean first = true;
        for (RsTypeParameter tp : typeParams) {
            if (!first) sb.append(", ");
            sb.append(tp.getText());
            first = false;
        }
        return createFromText("fn f() " + sb + " {}", RsWhereClause.class);
    }
}
