/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.PsiParserFacade;
import com.intellij.util.LocalTimeCounter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    @NotNull
    private final Project project;
    private final boolean markGenerated;
    private final boolean eventSystemEnabled;

    public RsPsiFactory(@NotNull Project project) {
        this(project, true, false);
    }

    public RsPsiFactory(@NotNull Project project, boolean markGenerated) {
        this(project, markGenerated, false);
    }

    public RsPsiFactory(@NotNull Project project, boolean markGenerated, boolean eventSystemEnabled) {
        this.project = project;
        this.markGenerated = markGenerated;
        this.eventSystemEnabled = eventSystemEnabled;
    }

    @NotNull
    public RsFile createFile(@NotNull CharSequence text) {
        return (RsFile) createPsiFile(text);
    }

    @NotNull
    public PsiFile createPsiFile(@NotNull CharSequence text) {
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
    public RsMacroBody createMacroBody(@NotNull String text) {
        return createFromText("macro_rules! m " + text, RsMacroBody.class);
    }

    @NotNull
    public RsMacroCall createMacroCall(
        @NotNull MacroExpansionContext context,
        @NotNull MacroBraces braces,
        @NotNull String macroName,
        @NotNull String... arguments
    ) {
        return createMacroCall(context, braces, macroName, String.join(", ", arguments));
    }

    @NotNull
    public RsMacroCall createMacroCall(
        @NotNull MacroExpansionContext context,
        @NotNull MacroBraces braces,
        @NotNull String macroName,
        @NotNull String argument
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

    @NotNull
    public RsFormatMacroArg createFormatMacroArg(@NotNull String argument) {
        RsFormatMacroArg result = createFromText("print!(" + argument + ")", RsFormatMacroArg.class);
        if (result == null) throw new IllegalStateException("Failed to create format macro argument");
        return result;
    }

    @NotNull
    public RsSelfParameter createSelf(boolean mutable) {
        RsFunction fn = createFromText("fn main(" + (mutable ? "mut " : "") + "self){}", RsFunction.class);
        if (fn == null || fn.getSelfParameter() == null) throw new IllegalStateException("Failed to create self element");
        return fn.getSelfParameter();
    }

    @NotNull
    public RsSelfParameter createSelfReference(boolean mutable) {
        RsFunction fn = createFromText("fn main(&" + (mutable ? "mut " : "") + "self){}", RsFunction.class);
        if (fn == null || fn.getSelfParameter() == null) throw new IllegalStateException("Failed to create self element");
        return fn.getSelfParameter();
    }

    @NotNull
    public RsSelfParameter createSelfWithType(@NotNull String text) {
        RsFunction fn = createFromText("fn main(self: " + text + "){}", RsFunction.class);
        if (fn == null || fn.getSelfParameter() == null) throw new IllegalStateException("Failed to create self element");
        return fn.getSelfParameter();
    }

    @NotNull
    public PsiElement createIdentifier(@NotNull String text) {
        RsModDeclItem modDecl = createFromText("mod " + RsRawIdentifiers.escapeIdentifierIfNeeded(text) + ";", RsModDeclItem.class);
        if (modDecl == null || modDecl.getIdentifier() == null) {
            throw new IllegalStateException("Failed to create identifier: `" + text + "`");
        }
        return modDecl.getIdentifier();
    }

    @NotNull
    public PsiElement createQuoteIdentifier(@NotNull String text) {
        RsLifetimeParameter param = createFromText("fn foo<" + text + ">(_: &" + text + " u8) {}", RsLifetimeParameter.class);
        if (param == null || param.getQuoteIdentifier() == null) {
            throw new IllegalStateException("Failed to create quote identifier: `" + text + "`");
        }
        return param.getQuoteIdentifier();
    }

    @NotNull
    public PsiElement createMetavarIdentifier(@NotNull String text) {
        RsMetaVarIdentifier result = createFromText("macro m { ($ " + text + ") => () }", RsMetaVarIdentifier.class);
        if (result == null) throw new IllegalStateException("Failed to create metavar identifier: `" + text + "`");
        return result;
    }

    @NotNull
    public RsExpr createExpression(@NotNull String text) {
        RsExpr result = tryCreateExpression(text);
        if (result == null) throw new IllegalStateException("Failed to create expression from text: `" + text + "`");
        return result;
    }

    @Nullable
    public RsExpr tryCreateExpression(@NotNull CharSequence text) {
        return createFromText("fn main() { let _ = " + text + "; }", RsExpr.class);
    }

    @Nullable
    public RsExprStmt tryCreateExprStmtWithSemicolon(@NotNull CharSequence text) {
        RsExprStmt stmt = createFromText("fn main() { " + text + "; }", RsExprStmt.class);
        return stmt != null && stmt.getTextLength() == text.length() + 1 ? stmt : null;
    }

    @Nullable
    public RsExprStmt tryCreateExprStmtWithoutSemicolon(@NotNull CharSequence text) {
        RsExprStmt stmt = createFromText("fn main() { " + text + " }", RsExprStmt.class);
        return stmt != null && stmt.getTextLength() == text.length() ? stmt : null;
    }

    @NotNull
    public RsTryExpr createTryExpression(@NotNull RsExpr expr) {
        RsTryExpr newElement = createExpressionOfType("a?", RsTryExpr.class);
        newElement.getExpr().replace(expr);
        return newElement;
    }

    @NotNull
    public RsIfExpr createIfExpression(@NotNull RsExpr condition, @NotNull RsExpr thenBranch) {
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

    @NotNull
    public RsIfExpr createIfElseExpression(@NotNull RsExpr condition, @NotNull RsBlock thenBlock, @NotNull RsBlock elseBlock) {
        RsIfExpr result = createExpressionOfType("if " + condition.getText() + " { () } else { () }", RsIfExpr.class);
        result.getBlock().replace(thenBlock);
        result.getElseBranch().getBlock().replace(elseBlock);
        return result;
    }

    @NotNull
    public RsBlockExpr createBlockExpr(@NotNull CharSequence body) {
        return createExpressionOfType("{ " + body + " }", RsBlockExpr.class);
    }

    @NotNull
    public RsElement createUnsafeBlockExprOrStmt(@NotNull PsiElement body) {
        if (body instanceof RsExpr) {
            return createExpressionOfType("unsafe { " + body.getText() + " }", RsBlockExpr.class);
        } else if (body instanceof RsStmt) {
            RsExprStmt result = createFromText("fn f() { unsafe { " + body.getText() + " } }", RsExprStmt.class);
            if (result == null) throw new IllegalStateException("Failed to create unsafe block");
            return result;
        }
        throw new IllegalStateException("Unsupported element type: " + body);
    }

    @NotNull
    public RsRetExpr createRetExpr(@NotNull String expr) {
        return createExpressionOfType("return " + expr, RsRetExpr.class);
    }

    @Nullable
    public RsPath tryCreatePath(@NotNull String text) {
        return tryCreatePath(text, PathParsingMode.TYPE);
    }

    @Nullable
    public RsPath tryCreatePath(@NotNull String text, @NotNull PathParsingMode ns) {
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

    @NotNull
    public RsStructLiteral createStructLiteral(@NotNull String name) {
        return createExpressionOfType(name + " { }", RsStructLiteral.class);
    }

    @NotNull
    public RsStructLiteral createStructLiteral(@NotNull String name, @NotNull String bodyText) {
        return createExpressionOfType(name + " " + bodyText, RsStructLiteral.class);
    }

    @NotNull
    public RsStructLiteral createStructLiteralWithFields(@NotNull String name, @NotNull String fields) {
        return createExpressionOfType(name + " " + fields, RsStructLiteral.class);
    }

    @NotNull
    public RsStructLiteralField createStructLiteralField(@NotNull String name, @NotNull String value) {
        return createExpressionOfType("S { " + name + ": " + value + " }", RsStructLiteral.class)
            .getStructLiteralBody()
            .getStructLiteralFieldList()
            .get(0);
    }

    @NotNull
    public RsStructLiteralField createStructLiteralField(@NotNull String name, @Nullable RsExpr value) {
        RsStructLiteralField field = createExpressionOfType("S { " + name + ": () }", RsStructLiteral.class)
            .getStructLiteralBody()
            .getStructLiteralFieldList()
            .get(0);
        if (value != null && field.getExpr() != null) {
            field.getExpr().replace(value);
        }
        return field;
    }

    @NotNull
    public RsNamedFieldDecl createStructNamedField(@NotNull String text) {
        RsNamedFieldDecl result = createFromText("struct S { " + text + " }", RsNamedFieldDecl.class);
        if (result == null) throw new IllegalStateException("Failed to create block fields");
        return result;
    }

    @NotNull
    public RsBlockFields createBlockFields(@NotNull List<BlockField> fields) {
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

    @NotNull
    public RsTupleFields createTupleFields(@NotNull List<TupleField> fields) {
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

    @NotNull
    public RsEnumVariant createEnumVariant(@NotNull String text) {
        RsEnumItem e = createFromText("enum E { " + text + " }", RsEnumItem.class);
        if (e == null || RsEnumItemUtil.getVariants(e).size() != 1) {
            throw new IllegalStateException("Failed to create enum variant from text: `" + text + "`");
        }
        return RsEnumItemUtil.getVariants(e).get(0);
    }

    @NotNull
    public RsStructItem createStruct(@NotNull String text) {
        RsStructItem result = tryCreateStruct(text);
        if (result == null) throw new IllegalStateException("Failed to create struct from text: `" + text + "`");
        return result;
    }

    @Nullable
    public RsStructItem tryCreateStruct(@NotNull String text) {
        return createFromText(text, RsStructItem.class);
    }

    @NotNull
    public RsStmt createStatement(@NotNull String text) {
        RsStmt result = createFromText("fn main() { " + text + " 92; }", RsStmt.class);
        if (result == null) throw new IllegalStateException("Failed to create statement from text: `" + text + "`");
        return result;
    }

    @NotNull
    public RsLetDecl createLetDeclaration(
        @NotNull String name,
        @Nullable RsExpr expr,
        boolean mutable,
        @Nullable RsTypeReference type
    ) {
        String mutStr = mutable ? "mut " : " ";
        String typeStr = type != null ? ": " + type.getText() : "";
        String exprStr = expr != null ? " = " + expr.getText() : "";
        return (RsLetDecl) createStatement("let " + mutStr + name + typeStr + exprStr + ";");
    }

    @NotNull
    public RsLetDecl createLetDeclaration(@NotNull String name, @Nullable RsExpr expr) {
        return createLetDeclaration(name, expr, false, null);
    }

    @NotNull
    public RsTypeReference createType(@NotNull CharSequence text) {
        RsTypeReference result = tryCreateType(text);
        if (result == null) throw new IllegalStateException("Failed to create type from text: `" + text + "`");
        return result;
    }

    @Nullable
    public RsTypeReference tryCreateType(@NotNull CharSequence text) {
        return createFromText("fn main() { let a : " + text + "; }", RsTypeReference.class);
    }

    @NotNull
    public PsiElement createMethodParam(@NotNull String text) {
        RsFunction fnItem = createTraitMethodMember("fn foo(" + text + ");");
        if (fnItem.getSelfParameter() != null) return fnItem.getSelfParameter();
        if (!fnItem.getValueParameters().isEmpty()) return fnItem.getValueParameters().get(0);
        throw new IllegalStateException("Failed to create method param from text: `" + text + "`");
    }

    @NotNull
    public RsRefLikeType createReferenceType(@NotNull String innerTypeText, @NotNull Mutability mutability) {
        return (RsRefLikeType) org.rust.lang.core.psi.ext.RsTypeReferenceUtil.skipParens(createType("&" + (mutability.isMut() ? "mut " : "") + innerTypeText));
    }

    @NotNull
    public RsModDeclItem createModDeclItem(@NotNull String modName) {
        RsModDeclItem result = tryCreateModDeclItem(modName);
        if (result == null) throw new IllegalStateException("Failed to create mod decl with name: `" + modName + "`");
        return result;
    }

    @Nullable
    public RsModDeclItem tryCreateModDeclItem(@NotNull String modName) {
        return createFromText("mod " + RsRawIdentifiers.escapeIdentifierIfNeeded(modName) + ";", RsModDeclItem.class);
    }

    @NotNull
    public RsUseItem createUseItem(@NotNull String text, @NotNull String visibility, @Nullable String alias) {
        String aliasText = alias != null && !alias.isEmpty() ? " as " + alias : "";
        RsUseItem result = createFromText(visibility + " use " + text + aliasText + ";", RsUseItem.class);
        if (result == null) throw new IllegalStateException("Failed to create use item from text: `" + text + "`");
        return result;
    }

    @NotNull
    public RsUseItem createUseItem(@NotNull String text) {
        return createUseItem(text, "", null);
    }

    @NotNull
    public RsUseSpeck createUseSpeck(@NotNull String text) {
        RsUseSpeck result = createFromText("use " + text + ";", RsUseSpeck.class);
        if (result == null) throw new IllegalStateException("Failed to create use speck from text: `" + text + "`");
        return result;
    }

    @NotNull
    public RsExternCrateItem createExternCrateItem(@NotNull String crateName) {
        RsExternCrateItem result = createFromText("extern crate " + crateName + ";", RsExternCrateItem.class);
        if (result == null) throw new IllegalStateException("Failed to create extern crate item: `" + crateName + "`");
        return result;
    }

    @NotNull
    public RsModItem createModItem(@NotNull String modName, @NotNull String modText) {
        RsModItem result = createFromText("mod " + modName + " {\n" + modText + "\n}", RsModItem.class);
        if (result == null) {
            throw new IllegalStateException("Failed to create mod item with name: `" + modName + "` from text: `" + modText + "`");
        }
        return result;
    }

    @NotNull
    public RsFunction createTraitMethodMember(@NotNull String text) {
        RsFunction result = createFromText("trait Foo { " + text + " }", RsFunction.class);
        if (result == null) throw new IllegalStateException("Failed to create method member from text: `" + text + "`");
        return result;
    }

    @NotNull
    public RsMembers createMembers(@NotNull String text) {
        RsMembers result = createFromText("impl T for S {" + text + "}", RsMembers.class);
        if (result == null) throw new IllegalStateException("Failed to create members from text: `" + text + "`");
        return result;
    }

    @Nullable
    public RsImplItem tryCreateImplItem(@NotNull String text) {
        return createFromText(text, RsImplItem.class);
    }

    @Nullable
    public RsTraitItem tryCreateTraitItem(@NotNull String text) {
        return createFromText(text, RsTraitItem.class);
    }

    @NotNull
    public RsImplItem createInherentImplItem(
        @NotNull String name,
        @Nullable RsTypeParameterList typeParameterList,
        @Nullable RsWhereClause whereClause
    ) {
        return createImplTemplate(name, typeParameterList, whereClause);
    }

    @NotNull
    public RsImplItem createInherentImplItem(@NotNull String name) {
        return createInherentImplItem(name, null, null);
    }

    @NotNull
    public RsImplItem createTraitImplItem(
        @NotNull String type,
        @NotNull String trait,
        @Nullable RsTypeParameterList typeParameterList,
        @Nullable RsWhereClause whereClause
    ) {
        return createImplTemplate(trait + " for " + type, typeParameterList, whereClause);
    }

    @NotNull
    private RsImplItem createImplTemplate(
        @NotNull String text,
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

    @NotNull
    public RsTypeParameterList createTypeParameterList(@NotNull Iterable<String> params) {
        String text = String.join(", ", params);
        RsFunction fn = createFromText("fn foo<" + text + ">() {}", RsFunction.class);
        if (fn == null || fn.getTypeParameterList() == null) {
            throw new IllegalStateException("Failed to create type parameter list from text: `" + text + "`");
        }
        return fn.getTypeParameterList();
    }

    @NotNull
    public RsTypeParameterList createTypeParameterList(@NotNull String params) {
        RsFunction fn = createFromText("fn foo<" + params + ">() {}", RsFunction.class);
        if (fn == null || fn.getTypeParameterList() == null) {
            throw new IllegalStateException("Failed to create type parameters from text: `<" + params + ">`");
        }
        return fn.getTypeParameterList();
    }

    @NotNull
    public RsOuterAttr createOuterAttr(@NotNull String text) {
        RsOuterAttr result = createFromText("#[" + text + "] struct Dummy;", RsOuterAttr.class);
        if (result == null) throw new IllegalStateException("Failed to create outer attr from text: `" + text + "`");
        return result;
    }

    @NotNull
    public RsInnerAttr createInnerAttr(@NotNull String text) {
        RsInnerAttr result = createFromText("#![" + text + "]", RsInnerAttr.class);
        if (result == null) throw new IllegalStateException("Failed to create inner attr from text: `" + text + "`");
        return result;
    }

    @NotNull
    public RsMetaItem createMetaItem(@NotNull String text) {
        RsMetaItem result = createFromText("#[" + text + "] fn f(){}", RsMetaItem.class);
        if (result == null) throw new IllegalStateException("Failed to create meta item from text: `" + text + "`");
        return result;
    }

    @NotNull
    public RsConstant createConstant(@NotNull String name, @NotNull RsExpr expr) {
        Ty exprType = org.rust.lang.core.psi.ext.RsExprUtil.getType(expr);
        String typeText = TypeRendering.renderInsertionSafe(exprType, true, false);
        RsConstant result = createFromText(
            "const " + name + ": " + typeText + " = " + expr.getText() + ";",
            RsConstant.class
        );
        if (result == null) throw new IllegalStateException("Failed to create constant " + name);
        return result;
    }

    @NotNull
    public RsVis createPub() {
        RsVis result = createFromText("pub fn f() {}", RsVis.class);
        if (result == null) throw new IllegalStateException("Failed to create `pub` element");
        return result;
    }

    @NotNull
    public RsVis createPubCrateRestricted() {
        RsVis result = createFromText("pub(crate) fn f() {}", RsVis.class);
        if (result == null) throw new IllegalStateException("Failed to create `pub(crate)` element");
        return result;
    }

    @NotNull
    public PsiComment createBlockComment(@NotNull String text) {
        return PsiParserFacade.getInstance(project).createBlockCommentFromText(RsLanguage.INSTANCE, text);
    }

    @NotNull
    public PsiComment createLineComment(@NotNull String text) {
        return PsiParserFacade.getInstance(project).createLineCommentFromText(RsFileType.INSTANCE, text);
    }

    @NotNull
    public PsiElement createComma() {
        RsValueParameter param = createFromText("fn f(_ : (), )", RsValueParameter.class);
        assert param != null;
        return param.getNextSibling();
    }

    @NotNull
    public PsiElement createSemicolon() {
        RsConstant c = createFromText("const C: () = ();", RsConstant.class);
        assert c != null && c.getSemicolon() != null;
        return c.getSemicolon();
    }

    @NotNull
    public PsiElement createColon() {
        RsConstant c = createFromText("const C: () = ();", RsConstant.class);
        assert c != null && c.getColon() != null;
        return c.getColon();
    }

    @NotNull
    public RsTypeParamBounds createTypeParamBounds(@NotNull String bound) {
        RsTraitItem trait = createFromText("trait T: " + bound + " {}", RsTraitItem.class);
        assert trait != null && trait.getTypeParamBounds() != null;
        return trait.getTypeParamBounds();
    }

    @NotNull
    public PsiElement createPlus() {
        RsTraitItem trait = createFromText("trait T: A + B {}", RsTraitItem.class);
        assert trait != null && trait.getTypeParamBounds() != null;
        java.util.List<RsPolybound> polybounds = trait.getTypeParamBounds().getPolyboundList();
        assert polybounds.size() >= 2;
        // The + is between the first polybound and the second
        com.intellij.psi.PsiElement next = polybounds.get(0).getNextSibling();
        while (next != null && next instanceof com.intellij.psi.PsiWhiteSpace) {
            next = next.getNextSibling();
        }
        assert next != null;
        return next;
    }

    @NotNull
    public RsPolybound createPolybound(@NotNull String bound) {
        RsTraitItem trait = createFromText("trait T: " + bound + " {}", RsTraitItem.class);
        assert trait != null && trait.getTypeParamBounds() != null;
        java.util.List<RsPolybound> polybounds = trait.getTypeParamBounds().getPolyboundList();
        assert !polybounds.isEmpty();
        return polybounds.get(0);
    }

    @NotNull
    public PsiElement createColonColon() {
        RsPath path = tryCreatePath("std::mem");
        assert path != null && path.getColoncolon() != null;
        return path.getColoncolon();
    }

    @NotNull
    public PsiElement createEq() {
        RsConstant c = createFromText("const C: () = ();", RsConstant.class);
        assert c != null && c.getEq() != null;
        return c.getEq();
    }

    @NotNull
    public PsiElement createNewline() {
        return createWhitespace("\n");
    }

    @NotNull
    public PsiElement createWhitespace(@NotNull String ws) {
        return PsiParserFacade.getInstance(project).createWhiteSpaceFromText(ws);
    }

    @NotNull
    public PsiElement createUnsafeKeyword() {
        RsFunction fn = createFromText("unsafe fn foo(){}", RsFunction.class);
        if (fn == null || fn.getUnsafe() == null) throw new IllegalStateException("Failed to create unsafe element");
        return fn.getUnsafe();
    }

    @NotNull
    public RsFunction createFunction(@NotNull String text) {
        RsFunction result = createFromText(text, RsFunction.class);
        if (result == null) throw new IllegalStateException("Failed to create function element: " + text);
        return result;
    }

    @NotNull
    public RsRetType createRetType(@NotNull String ty) {
        RsRetType result = createFromText("fn foo() -> " + ty + " {}", RsRetType.class);
        if (result == null) throw new IllegalStateException("Failed to create function return type: " + ty);
        return result;
    }

    @NotNull
    public RsValueParameterList createSimpleValueParameterList(@NotNull String name, @NotNull RsTypeReference type) {
        RsFunction fn = createFromText("fn main(" + name + ": " + type.getText() + "){}", RsFunction.class);
        if (fn == null || fn.getValueParameterList() == null) {
            throw new IllegalStateException("Failed to create parameter element");
        }
        return fn.getValueParameterList();
    }

    @NotNull
    public RsPatBinding createPatBinding(@NotNull String name, boolean mutable, boolean ref) {
        String refStr = ref ? "ref " : "";
        String mutStr = mutable ? "mut " : "";
        RsLetDecl letDecl = (RsLetDecl) createStatement("let " + refStr + mutStr + name + " = 10;");
        RsPat pat = letDecl.getPat();
        if (pat == null || !(pat.getFirstChild() instanceof RsPatBinding)) {
            throw new IllegalStateException("Failed to create pat element");
        }
        return (RsPatBinding) pat.getFirstChild();
    }

    @NotNull
    public RsPatBinding createPatBinding(@NotNull String name) {
        return createPatBinding(name, false, false);
    }

    @NotNull
    public RsPat createPat(@NotNull String patText) {
        RsPat result = tryCreatePat(patText);
        if (result == null) throw new IllegalStateException("Failed to create pat element");
        return result;
    }

    @Nullable
    public RsPat tryCreatePat(@NotNull String patText) {
        return ((RsLetDecl) createStatement("let " + patText + ";")).getPat();
    }

    @NotNull
    public RsTraitType createDynTraitType(@NotNull String pathText) {
        RsTraitType result = createFromText("type T = &dyn " + pathText + ";}", RsTraitType.class);
        if (result == null) throw new IllegalStateException("Failed to create trait type");
        return result;
    }

    @NotNull
    public RsAssocTypeBinding createAssocTypeBinding(@NotNull String name, @NotNull String type) {
        RsAssocTypeBinding result = createFromText("type T = &dyn Trait<" + name + "=" + type + ">;", RsAssocTypeBinding.class);
        if (result == null) throw new IllegalStateException("Failed to create assoc type binding");
        return result;
    }

    @NotNull
    public RsCastExpr createCastExpr(@NotNull RsExpr expr, @NotNull String typeText) {
        if (expr instanceof RsBinaryExpr) {
            return createExpressionOfType("(" + expr.getText() + ") as " + typeText, RsCastExpr.class);
        }
        return createExpressionOfType(expr.getText() + " as " + typeText, RsCastExpr.class);
    }

    @NotNull
    public RsCallExpr createFunctionCall(@NotNull String functionName, @NotNull Iterable<? extends RsExpr> arguments) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (RsExpr arg : arguments) {
            if (!first) sb.append(", ");
            sb.append(arg.getText());
            first = false;
        }
        return createExpressionOfType(functionName + "(" + sb + ")", RsCallExpr.class);
    }

    @NotNull
    public RsCallExpr createFunctionCall(@NotNull String functionName, @NotNull String arguments) {
        return createExpressionOfType(functionName + "(" + arguments + ")", RsCallExpr.class);
    }

    @NotNull
    public RsDotExpr createNoArgsMethodCall(@NotNull RsExpr expr, @NotNull String methodNameText) {
        if (expr instanceof RsBinaryExpr || expr instanceof RsUnaryExpr || expr instanceof RsCastExpr) {
            return createExpressionOfType("(" + expr.getText() + ")." + methodNameText + "()", RsDotExpr.class);
        }
        return createExpressionOfType(expr.getText() + "." + methodNameText + "()", RsDotExpr.class);
    }

    @NotNull
    public RsVisRestriction createVisRestriction(@NotNull String pathText) {
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
    public RsVis tryCreateVis(@NotNull String text) {
        return createFromText(text + " fn foo() {}", RsVis.class);
    }

    @NotNull
    public RsVis createVis(@NotNull String text) {
        RsVis result = tryCreateVis(text);
        if (result == null) throw new IllegalStateException("Failed to create vis");
        return result;
    }

    @NotNull
    public RsLabelDecl createLabelDeclaration(@NotNull String name) {
        RsLabelDecl result = createFromText("fn main() { '" + name + ": while true {} }", RsLabelDecl.class);
        if (result == null) throw new IllegalStateException("Failed to create label decl");
        return result;
    }

    @NotNull
    public RsLabel createLabel(@NotNull String name) {
        RsLabel result = createFromText("fn main() { break '" + name + "; }", RsLabel.class);
        if (result == null) throw new IllegalStateException("Failed to create label");
        return result;
    }

    // --- Helper methods ---

    @SuppressWarnings("unchecked")
    @Nullable
    private <T extends RsElement> T createFromText(@NotNull CharSequence code, @NotNull Class<T> clazz) {
        PsiFile file = createFile(code);
        return RsPsiJavaUtil.descendantOfTypeStrict(file, clazz);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private <E extends RsExpr> E createExpressionOfType(@NotNull String text, @NotNull Class<E> clazz) {
        RsExpr expr = createExpression(text);
        if (!clazz.isInstance(expr)) {
            throw new IllegalStateException("Failed to create " + clazz.getSimpleName() + " from `" + text + "`");
        }
        return clazz.cast(expr);
    }

    // --- Data classes ---
    public static final class BlockField {
        @NotNull public final String name;
        @NotNull public final Ty type;
        public final boolean addPub;

        public BlockField(@NotNull String name, @NotNull Ty type, boolean addPub) {
            this.name = name;
            this.type = type;
            this.addPub = addPub;
        }
    }

    public static final class TupleField {
        @NotNull public final Ty type;
        public final boolean addPub;

        public TupleField(@NotNull Ty type, boolean addPub) {
            this.type = type;
            this.addPub = addPub;
        }
    }

    // ===== Additional factory methods needed by IDE code =====

    @NotNull
    public PsiElement createAsyncKeyword() {
        return createFromText("async fn f() {}", RsFunction.class).getFirstChild();
    }

    @NotNull
    public RsCallExpr createAssocFunctionCall(@NotNull String typeName, @NotNull String methodName, @NotNull String... args) {
        String argsStr = String.join(", ", args);
        return createFromText(typeName + "::" + methodName + "(" + argsStr + ");", RsCallExpr.class);
    }

    @NotNull
    public RsCallExpr createAssocFunctionCall(@NotNull String typeName, @NotNull String methodName, @NotNull List<RsExpr> args) {
        StringBuilder sb = new StringBuilder(typeName).append("::").append(methodName).append("(");
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(args.get(i).getText());
        }
        sb.append(")");
        return createFromText(sb + ";", RsCallExpr.class);
    }

    @NotNull
    public RsTypeArgumentList createTypeArgumentList(@NotNull List<String> types) {
        String typeArgs = "<" + String.join(", ", types) + ">";
        return createFromText("fn f() -> Foo" + typeArgs + " {}", RsTypeArgumentList.class);
    }

    @Nullable
    public RsMethodCall tryCreateMethodCall(@NotNull RsExpr receiver, @NotNull String methodName, @NotNull List<RsExpr> args) {
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

    @NotNull
    public RsValueParameter createValueParameter(@NotNull String name, @NotNull RsTypeReference typeRef, boolean mutable, @Nullable RsLifetime lifetime) {
        String mutStr = mutable ? "mut " : "";
        return createFromText("fn f(" + mutStr + name + ": " + typeRef.getText() + ") {}", RsValueParameter.class);
    }

    @Nullable
    public RsValueParameter tryCreateValueParameter(@NotNull String patText, @Nullable RsTypeReference typeRef, boolean mutable) {
        String mutStr = mutable ? "mut " : "";
        String typeStr = typeRef != null ? ": " + typeRef.getText() : ": ()";
        return createFromText("fn f(" + mutStr + patText + typeStr + ") {}", RsValueParameter.class);
    }

    @Nullable
    public RsValueParameter tryCreateValueParameter(@NotNull String patText, @Nullable RsTypeReference typeRef) {
        return tryCreateValueParameter(patText, typeRef, false);
    }

    @NotNull
    public RsPatRest createPatRest() {
        return createFromText("let (..) = x;", RsPatRest.class);
    }

    @NotNull
    public RsMatchBody createMatchBody(@NotNull List<?> arms) {
        StringBuilder sb = new StringBuilder("match x {");
        for (Object arm : arms) {
            sb.append(arm.toString()).append(",");
        }
        sb.append("}");
        return createFromText(sb.toString(), RsMatchBody.class);
    }

    @NotNull
    public RsExpr createRefExpr(@NotNull RsExpr expr, @NotNull java.util.List<org.rust.lang.core.types.ty.Mutability> refs) {
        StringBuilder sb = new StringBuilder();
        for (org.rust.lang.core.types.ty.Mutability mut : refs) {
            sb.append(mut.isMut() ? "&mut " : "&");
        }
        sb.append(expr.getText());
        return createExpression(sb.toString());
    }

    @NotNull
    public RsExpr createDerefExpr(@NotNull RsExpr expr, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.append("*");
        sb.append(expr.getText());
        return createExpression(sb.toString());
    }

    @Nullable
    public RsValueArgumentList tryCreateValueArgumentList(@NotNull List<RsExpr> args) {
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

    @NotNull
    public PsiElement createIn() {
        RsForExpr forExpr = createFromText("for x in y {}", RsForExpr.class);
        if (forExpr == null) throw new IllegalStateException("Failed to create for expression");
        PsiElement in = forExpr.getIn();
        if (in == null) throw new IllegalStateException("Failed to find 'in' keyword");
        return in;
    }

    @NotNull
    public RsExpr createBox(@NotNull String text) {
        return createExpression("Box::new(" + text + ")");
    }

    @NotNull
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

    @NotNull
    public RsExpr createLoop(@NotNull String label, @NotNull String body) {
        String labelStr = label.isEmpty() ? "" : label + ": ";
        return createExpression(labelStr + "loop { " + body + " }");
    }

    @NotNull
    public RsLambdaExpr createLambda(@NotNull String text) {
        return createFromText("let _ = " + text + ";", RsLambdaExpr.class);
    }

    @NotNull
    public RsPatTupleStruct createPatTupleStruct(@NotNull RsStructItem struct, @NotNull String name) {
        return createFromText("let " + name + "(..) = x;", RsPatTupleStruct.class);
    }

    @NotNull
    public RsPatTupleStruct createPatTupleStruct(@NotNull String name, @NotNull java.util.List<? extends RsPat> pats) {
        StringBuilder sb = new StringBuilder();
        sb.append("let ").append(name).append("(");
        for (int i = 0; i < pats.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(pats.get(i).getText());
        }
        sb.append(") = x;");
        return createFromText(sb.toString(), RsPatTupleStruct.class);
    }

    @NotNull
    public RsPatStruct createPatStruct(@NotNull RsStructItem struct, @NotNull String name) {
        return createFromText("let " + name + " { .. } = x;", RsPatStruct.class);
    }

    @NotNull
    public RsPatStruct createPatStruct(@NotNull String name, @NotNull java.util.List<? extends RsPatField> fields, @org.jetbrains.annotations.Nullable RsPatRest patRest) {
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

    @NotNull
    public RsPatTup createPatTuple(int size) {
        StringBuilder sb = new StringBuilder("let (");
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(", ");
            sb.append("_");
        }
        sb.append(") = x;");
        return createFromText(sb.toString(), RsPatTup.class);
    }

    @NotNull
    public RsPatField createPatField(@NotNull String fieldName) {
        return createFromText("let Foo { " + fieldName + " } = x;", RsPatField.class);
    }

    @NotNull
    public RsPatFieldFull createPatFieldFull(@NotNull String fieldName, @NotNull String patText) {
        return createFromText("let Foo { " + fieldName + ": " + patText + " } = x;", RsPatFieldFull.class);
    }

    @NotNull
    public RsWhereClause createWhereClause(@NotNull List<RsLifetimeParameter> lifetimeParams, @NotNull List<RsTypeParameter> typeParams) {
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
