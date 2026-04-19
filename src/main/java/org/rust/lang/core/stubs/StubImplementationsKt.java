/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.lang.*;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.lang.FileASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.StubBuilder;
import com.intellij.psi.impl.source.tree.LazyParseableElement;
import com.intellij.psi.impl.source.tree.RecursiveTreeElementWalkingVisitor;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.*;
import com.intellij.util.BitUtil;
import com.intellij.util.CharTable;
import com.intellij.util.diff.FlyweightCapableTreeStructure;
import com.intellij.util.io.DataInputOutputUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.lexer.RsLexer;
import org.rust.lang.core.parser.RustParser;
import org.rust.lang.core.parser.RustParserDefinition;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.stubs.common.RsMetaItemArgsPsiOrStub;
import org.rust.lang.core.stubs.common.RsMetaItemPsiOrStub;
import org.rust.lang.core.stubs.common.RsPathPsiOrStub;
import org.rust.lang.core.types.ty.TyFloat;
import org.rust.lang.core.types.ty.TyInteger;
import org.rust.stdext.BitFlagsBuilder;
import org.rust.stdext.HashCode;

import org.rust.openapiext.AstExt;

import java.io.IOException;

import static org.rust.lang.core.psi.RsElementTypes.*;
import static org.rust.lang.core.psi.RsTokenType.*;

/**
 * Contains all stub implementation classes and the factory method.
 */
public final class StubImplementationsKt {

    private StubImplementationsKt() {
    }

    @NotNull
    public static RsStubElementType<?, ?> factory(@NotNull String name) {
        switch (name) {
            case "EXTERN_CRATE_ITEM": return RsExternCrateItemStub.Type;
            case "USE_ITEM": return RsUseItemStub.Type;
            case "STRUCT_ITEM": return RsStructItemStub.Type;
            case "ENUM_ITEM": return RsEnumItemStub.Type;
            case "ENUM_BODY": return new RsPlaceholderStub.Type<>("ENUM_BODY", RsEnumBodyImpl::new);
            case "ENUM_VARIANT": return RsEnumVariantStub.Type;
            case "MOD_DECL_ITEM": return RsModDeclItemStub.Type;
            case "MOD_ITEM": return RsModItemStub.Type;
            case "TRAIT_ITEM": return RsTraitItemStub.Type;
            case "IMPL_ITEM": return RsImplItemStub.Type;
            case "MEMBERS": return new RsPlaceholderStub.Type<>("MEMBERS", RsMembersImpl::new);
            case "TRAIT_ALIAS": return RsTraitAliasStub.Type;
            case "TRAIT_ALIAS_BOUNDS": return new RsPlaceholderStub.Type<>("TRAIT_ALIAS_BOUNDS", RsTraitAliasBoundsImpl::new);
            case "FUNCTION": return RsFunctionStub.Type;
            case "CONSTANT": return RsConstantStub.Type;
            case "TYPE_ALIAS": return RsTypeAliasStub.Type;
            case "FOREIGN_MOD_ITEM": return RsForeignModStub.Type;
            case "BLOCK_FIELDS": return new RsPlaceholderStub.Type<>("BLOCK_FIELDS", RsBlockFieldsImpl::new);
            case "TUPLE_FIELDS": return new RsPlaceholderStub.Type<>("TUPLE_FIELDS", RsTupleFieldsImpl::new);
            case "TUPLE_FIELD_DECL": return new RsPlaceholderStub.Type<>("TUPLE_FIELD_DECL", RsTupleFieldDeclImpl::new);
            case "NAMED_FIELD_DECL": return RsNamedFieldDeclStub.Type;
            case "ALIAS": return RsAliasStub.Type;
            case "USE_SPECK": return RsUseSpeckStub.Type;
            case "USE_GROUP": return new RsPlaceholderStub.Type<>("USE_GROUP", RsUseGroupImpl::new);
            case "PATH": return RsPathStub.Type;
            case "TYPE_QUAL": return new RsPlaceholderStub.Type<>("TYPE_QUAL", RsTypeQualImpl::new);
            case "TRAIT_REF": return new RsPlaceholderStub.Type<>("TRAIT_REF", RsTraitRefImpl::new);
            case "TYPE_REFERENCE": return new RsPlaceholderStub.Type<>("TYPE_REFERENCE", RsTypeReferenceImpl::new);
            case "ARRAY_TYPE": return RsArrayTypeStub.Type;
            case "REF_LIKE_TYPE": return RsRefLikeTypeStub.Type;
            case "FN_POINTER_TYPE": return RsFnPointerTypeStub.Type;
            case "TUPLE_TYPE": return new RsPlaceholderStub.Type<>("TUPLE_TYPE", RsTupleTypeImpl::new);
            case "PAREN_TYPE": return new RsPlaceholderStub.Type<>("PAREN_TYPE", RsParenTypeImpl::new);
            case "UNIT_TYPE": return new RsPlaceholderStub.Type<>("UNIT_TYPE", RsUnitTypeImpl::new);
            case "NEVER_TYPE": return new RsPlaceholderStub.Type<>("NEVER_TYPE", RsNeverTypeImpl::new);
            case "INFER_TYPE": return new RsPlaceholderStub.Type<>("INFER_TYPE", RsInferTypeImpl::new);
            case "PATH_TYPE": return RsPathTypeStub.Type;
            case "FOR_IN_TYPE": return new RsPlaceholderStub.Type<>("FOR_IN_TYPE", RsForInTypeImpl::new);
            case "TRAIT_TYPE": return RsTraitTypeStub.Type;
            case "MACRO_TYPE": return new RsPlaceholderStub.Type<>("MACRO_TYPE", RsMacroTypeImpl::new);
            case "VALUE_PARAMETER_LIST": return new RsPlaceholderStub.Type<>("VALUE_PARAMETER_LIST", RsValueParameterListImpl::new);
            case "VALUE_PARAMETER": return RsValueParameterStub.Type;
            case "SELF_PARAMETER": return RsSelfParameterStub.Type;
            case "VARIADIC": return new RsPlaceholderStub.Type<>("VARIADIC", RsVariadicImpl::new);
            case "TYPE_PARAMETER_LIST": return new RsPlaceholderStub.Type<>("TYPE_PARAMETER_LIST", RsTypeParameterListImpl::new);
            case "TYPE_PARAMETER": return RsTypeParameterStub.Type;
            case "CONST_PARAMETER": return RsConstParameterStub.Type;
            case "LIFETIME": return RsLifetimeStub.Type;
            case "LIFETIME_PARAMETER": return RsLifetimeParameterStub.Type;
            case "FOR_LIFETIMES": return new RsPlaceholderStub.Type<>("FOR_LIFETIMES", RsForLifetimesImpl::new);
            case "TYPE_ARGUMENT_LIST": return new RsPlaceholderStub.Type<>("TYPE_ARGUMENT_LIST", RsTypeArgumentListImpl::new);
            case "ASSOC_TYPE_BINDING": return new RsPlaceholderStub.Type<>("ASSOC_TYPE_BINDING", RsAssocTypeBindingImpl::new);
            case "TYPE_PARAM_BOUNDS": return new RsPlaceholderStub.Type<>("TYPE_PARAM_BOUNDS", RsTypeParamBoundsImpl::new);
            case "POLYBOUND": return RsPolyboundStub.Type;
            case "BOUND": return new RsPlaceholderStub.Type<>("BOUND", RsBoundImpl::new);
            case "WHERE_CLAUSE": return new RsPlaceholderStub.Type<>("WHERE_CLAUSE", RsWhereClauseImpl::new);
            case "WHERE_PRED": return new RsPlaceholderStub.Type<>("WHERE_PRED", RsWherePredImpl::new);
            case "RET_TYPE": return new RsPlaceholderStub.Type<>("RET_TYPE", RsRetTypeImpl::new);
            case "MACRO": return RsMacroStub.Type;
            case "MACRO_2": return RsMacro2Stub.Type;
            case "MACRO_CALL": return RsMacroCallStub.Type;
            case "INCLUDE_MACRO_ARGUMENT": return new RsPlaceholderStub.Type<>("INCLUDE_MACRO_ARGUMENT", RsIncludeMacroArgumentImpl::new);
            case "CONCAT_MACRO_ARGUMENT": return new RsPlaceholderStub.Type<>("CONCAT_MACRO_ARGUMENT", RsConcatMacroArgumentImpl::new);
            case "ENV_MACRO_ARGUMENT": return new RsPlaceholderStub.Type<>("ENV_MACRO_ARGUMENT", RsEnvMacroArgumentImpl::new);
            case "INNER_ATTR": return RsInnerAttrStub.Type;
            case "OUTER_ATTR": return new RsPlaceholderStub.Type<>("OUTER_ATTR", RsOuterAttrImpl::new);
            case "META_ITEM": return RsMetaItemStub.Type;
            case "META_ITEM_ARGS": return RsMetaItemArgsStub.Type;
            case "BLOCK": return RsBlockStubType.INSTANCE;
            case "BINARY_OP": return RsBinaryOpStub.Type;
            case "EXPR_STMT": return RsExprStmtStub.Type;
            case "LET_DECL": return RsLetDeclStub.Type;
            case "EMPTY_STMT": return RsEmptyStmtType.INSTANCE;
            case "ARRAY_EXPR": return new RsExprStubType<>("ARRAY_EXPR", RsArrayExprImpl::new);
            case "BINARY_EXPR": return new RsExprStubType<>("BINARY_EXPR", RsBinaryExprImpl::new);
            case "BLOCK_EXPR": return RsBlockExprStub.Type;
            case "BREAK_EXPR": return new RsExprStubType<>("BREAK_EXPR", RsBreakExprImpl::new);
            case "CALL_EXPR": return new RsExprStubType<>("CALL_EXPR", RsCallExprImpl::new);
            case "CAST_EXPR": return new RsExprStubType<>("CAST_EXPR", RsCastExprImpl::new);
            case "CONT_EXPR": return new RsExprStubType<>("CONT_EXPR", RsContExprImpl::new);
            case "DOT_EXPR": return new RsExprStubType<>("DOT_EXPR", RsDotExprImpl::new);
            case "FOR_EXPR": return new RsExprStubType<>("FOR_EXPR", RsForExprImpl::new);
            case "IF_EXPR": return new RsExprStubType<>("IF_EXPR", RsIfExprImpl::new);
            case "LET_EXPR": return new RsExprStubType<>("LET_EXPR", RsLetExprImpl::new);
            case "INDEX_EXPR": return new RsExprStubType<>("INDEX_EXPR", RsIndexExprImpl::new);
            case "LAMBDA_EXPR": return new RsExprStubType<>("LAMBDA_EXPR", RsLambdaExprImpl::new);
            case "LIT_EXPR": return RsLitExprStub.Type;
            case "LOOP_EXPR": return new RsExprStubType<>("LOOP_EXPR", RsLoopExprImpl::new);
            case "MACRO_EXPR": return new RsExprStubType<>("MACRO_EXPR", RsMacroExprImpl::new);
            case "MATCH_EXPR": return new RsExprStubType<>("MATCH_EXPR", RsMatchExprImpl::new);
            case "PAREN_EXPR": return new RsExprStubType<>("PAREN_EXPR", RsParenExprImpl::new);
            case "PATH_EXPR": return new RsExprStubType<>("PATH_EXPR", RsPathExprImpl::new);
            case "RANGE_EXPR": return new RsExprStubType<>("RANGE_EXPR", RsRangeExprImpl::new);
            case "RET_EXPR": return new RsExprStubType<>("RET_EXPR", RsRetExprImpl::new);
            case "YIELD_EXPR": return new RsExprStubType<>("YIELD_EXPR", RsYieldExprImpl::new);
            case "STRUCT_LITERAL": return new RsExprStubType<>("STRUCT_LITERAL", RsStructLiteralImpl::new);
            case "TRY_EXPR": return new RsExprStubType<>("TRY_EXPR", RsTryExprImpl::new);
            case "TUPLE_EXPR": return new RsExprStubType<>("TUPLE_EXPR", RsTupleExprImpl::new);
            case "UNARY_EXPR": return RsUnaryExprStub.Type;
            case "UNIT_EXPR": return new RsExprStubType<>("UNIT_EXPR", RsUnitExprImpl::new);
            case "WHILE_EXPR": return new RsExprStubType<>("WHILE_EXPR", RsWhileExprImpl::new);
            case "UNDERSCORE_EXPR": return new RsExprStubType<>("UNDERSCORE_EXPR", RsUnderscoreExprImpl::new);
            case "PREFIX_INC_EXPR": return new RsExprStubType<>("PREFIX_INC_EXPR", RsPrefixIncExprImpl::new);
            case "POSTFIX_INC_EXPR": return new RsExprStubType<>("POSTFIX_INC_EXPR", RsPostfixIncExprImpl::new);
            case "POSTFIX_DEC_EXPR": return new RsExprStubType<>("POSTFIX_DEC_EXPR", RsPostfixDecExprImpl::new);
            case "INC": return new RsPlaceholderStub.Type<>("INC", RsIncImpl::new);
            case "DEC": return new RsPlaceholderStub.Type<>("DEC", RsDecImpl::new);
            case "VIS": return RsVisStub.Type;
            case "VIS_RESTRICTION": return new RsPlaceholderStub.Type<>("VIS_RESTRICTION", RsVisRestrictionImpl::new);
            case "DEFAULT_PARAMETER_VALUE": return new RsPlaceholderStub.Type<>("DEFAULT_PARAMETER_VALUE", RsDefaultParameterValueImpl::new);
            default:
                throw new IllegalArgumentException("Unknown element " + name);
        }
    }

    // Helper methods for StubInputStream/StubOutputStream

    @Nullable
    static String readNameAsString(@NotNull StubInputStream dataStream) throws IOException {
        com.intellij.util.io.StringRef ref = dataStream.readName();
        return ref != null ? ref.getString() : null;
    }

    @Nullable
    static String readUTFFastAsNullable(@NotNull StubInputStream dataStream) throws IOException {
        return DataInputOutputUtil.readNullable(dataStream, dataStream::readUTFFast);
    }

    static void writeUTFFastAsNullable(@NotNull StubOutputStream dataStream, @Nullable String value) throws IOException {
        DataInputOutputUtil.writeNullable(dataStream, value, dataStream::writeUTFFast);
    }

    @Nullable
    static Long readLongAsNullable(@NotNull StubInputStream dataStream) throws IOException {
        return DataInputOutputUtil.readNullable(dataStream, dataStream::readLong);
    }

    static void writeLongAsNullable(@NotNull StubOutputStream dataStream, @Nullable Long value) throws IOException {
        DataInputOutputUtil.writeNullable(dataStream, value, dataStream::writeLong);
    }

    @Nullable
    static Double readDoubleAsNullable(@NotNull StubInputStream dataStream) throws IOException {
        return DataInputOutputUtil.readNullable(dataStream, dataStream::readDouble);
    }

    static void writeDoubleAsNullable(@NotNull StubOutputStream dataStream, @Nullable Double value) throws IOException {
        DataInputOutputUtil.writeNullable(dataStream, value, dataStream::writeDouble);
    }

    // Helper methods for checking if an ASTNode is a function body
    static boolean isFunctionBody(@NotNull ASTNode node) {
        return node.getElementType() == BLOCK && node.getTreeParent() != null && node.getTreeParent().getElementType() == FUNCTION;
    }

    static boolean shouldCreateExprStub(@NotNull ASTNode node) {
        ASTNode element = null;
        for (ASTNode ancestor : AstExt.ancestors(node)) {
            ASTNode parent = ancestor.getTreeParent();
            if (parent != null && (RS_ITEMS.contains(parent.getElementType()) || parent instanceof FileASTNode)) {
                element = ancestor;
                break;
            }
        }
        return element != null && !isFunctionBody(element) && RsStubElementType.createStubIfParentIsStub(node);
    }

    static boolean shouldCreateStmtStub(@NotNull ASTNode node) {
        return shouldCreateExprStub(node)
            || node.findChildByType(OUTER_ATTR) != null && ItemSeekingVisitor.containsItems(node);
    }
}
