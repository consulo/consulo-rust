/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.cache.impl.IndexPatternUtil;
import com.intellij.psi.impl.search.PsiTodoSearchHelperImpl;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.search.PsiTodoSearchHelper;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.colors.RsColor;
import org.rust.ide.highlight.RsHighlighter;
import org.rust.ide.todo.RsTodoSearcher;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.macros.MacrosUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.ty.TyPrimitive;
import org.rust.openapiext.OpenApiUtil;

import java.util.List;
import org.rust.lang.core.psi.ext.RsStructItemUtil;
import org.rust.lang.core.psi.ext.RsConstantUtil;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;
import org.rust.lang.core.psi.ext.RsPatBindingUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;

public class RsHighlightingAnnotator extends AnnotatorBase {

    private static final Key<Boolean> IS_TODO_HIGHLIGHTING_ENABLED = Key.create("IS_TODO_HIGHLIGHTING_ENABLED");
    private static final TokenSet HIGHLIGHTED_ELEMENTS = TokenSet.orSet(
        RsTokenType.tokenSetOf(
            RsElementTypes.DOLLAR, RsElementTypes.IDENTIFIER, RsElementTypes.QUOTE_IDENTIFIER,
            RsElementTypes.SELF, RsElementTypes.FLOAT_LITERAL, RsElementTypes.Q, RsElementTypes.COLON,
            RsElementTypes.MUL, RsElementTypes.PLUS, RsElementTypes.LPAREN, RsElementTypes.LBRACE,
            RsElementTypes.RPAREN, RsElementTypes.RBRACE, RsElementTypes.EXCL
        ),
        RsTokenType.RS_CONTEXTUAL_KEYWORDS, RsTokenType.RS_LITERALS
    );

    @Override
    protected void annotateInternal(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (holder.isBatchMode()) return;
        if (!(element instanceof LeafPsiElement)) return;
        IElementType elementType = RsElementUtil.getElementType(element);
        if (!HIGHLIGHTED_ELEMENTS.contains(elementType)) return;

        RsColor color = highlightLeafInMacroCallBody(element, holder);
        if (color == null) {
            color = highlightLeafOutsideOfMacroCallBody(element, elementType, holder);
        }
        if (color == null) return;

        HighlightSeverity severity = OpenApiUtil.isUnitTestMode() ? color.getTestSeverity() : HighlightSeverity.INFORMATION;
        holder.newSilentAnnotation(severity).textAttributes(color.getTextAttributesKey()).create();
    }

    @Nullable
    private RsColor highlightLeafInMacroCallBody(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        List<PsiElement> expansionElements = MacrosUtil.findExpansionElements(element, AnnotationSessionEx.attrCache(holder));
        if (expansionElements == null) return null;

        for (PsiElement expansionElement : expansionElements) {
            if (!(expansionElement instanceof LeafPsiElement)) continue;
            RsColor color = highlightLeaf(expansionElement, RsElementUtil.getElementType(expansionElement), holder);
            if (color == null) continue;
            if (!shouldHighlightElement(expansionElement, holder)) continue;
            return color;
        }
        return null;
    }

    @Nullable
    private RsColor highlightLeafOutsideOfMacroCallBody(@NotNull PsiElement element, @NotNull IElementType elementType, @NotNull AnnotationHolder holder) {
        RsColor color = highlightLeaf(element, elementType, holder);
        if (color == null) return null;
        if (!shouldHighlightElement(element, holder)) return null;
        return color;
    }

    private boolean shouldHighlightElement(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        Crate crate = AnnotationSessionEx.currentCrate(holder);
        if (crate == null) return true;
        if (!RsElementUtil.existsAfterExpansion(element, crate)) return false;
        for (PsiElement ancestor : RsElementUtil.getAncestors(element)) {
            if (ancestor instanceof RsAttr && RsAttrExtUtil.isDisabledCfgAttrAttribute((RsAttr) ancestor, crate)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private RsColor highlightLeaf(@NotNull PsiElement element, @NotNull IElementType elementType, @NotNull AnnotationHolder holder) {
        PsiElement parent = element.getParent();
        if (!(parent instanceof RsElement)) return null;
        RsElement rsParent = (RsElement) parent;

        if (elementType == RsElementTypes.DOLLAR) return RsColor.MACRO;
        if (elementType == RsElementTypes.IDENTIFIER || elementType == RsElementTypes.QUOTE_IDENTIFIER || elementType == RsElementTypes.SELF) {
            return highlightIdentifier(element, rsParent, holder);
        }
        if (RsTokenType.RS_CONTEXTUAL_KEYWORDS.contains(elementType)) return RsColor.KEYWORD;
        if (elementType == RsElementTypes.FLOAT_LITERAL) return RsColor.NUMBER;
        if (elementType == RsElementTypes.Q) {
            return rsParent instanceof RsTryExpr ? RsColor.Q_OPERATOR : macroGroupColor(rsParent);
        }
        if (elementType == RsElementTypes.COLON) {
            return rsParent instanceof RsMacroBinding ? RsColor.MACRO : null;
        }
        if (elementType == RsElementTypes.MUL || elementType == RsElementTypes.PLUS
            || elementType == RsElementTypes.LPAREN || elementType == RsElementTypes.LBRACE
            || elementType == RsElementTypes.RPAREN || elementType == RsElementTypes.RBRACE) {
            return macroGroupColor(rsParent);
        }
        if (elementType == RsElementTypes.EXCL) {
            if (rsParent instanceof RsMacro) return RsColor.MACRO;
            if (rsParent instanceof RsMacroCall && shouldHighlightMacroCall((RsMacroCall) rsParent, holder)) return RsColor.MACRO;
            return null;
        }
        if (RsTokenType.RS_LITERALS.contains(elementType)) {
            if (rsParent instanceof RsLitExpr) {
                PsiElement grandParent = rsParent.getParent();
                if (grandParent instanceof RsMetaItem || grandParent instanceof RsMetaItemArgs) {
                    return RsHighlighter.map(elementType);
                }
            }
            return null;
        }
        return null;
    }

    @Nullable
    private RsColor highlightIdentifier(@NotNull PsiElement element, @NotNull RsElement parent, @NotNull AnnotationHolder holder) {
        if (parent instanceof RsReferenceElement && !(parent instanceof RsModDeclItem)
            && (!(parent instanceof RsPatBinding) || RsPatBindingUtil.isReferenceToConstant((RsPatBinding) parent))
            && element == ((RsReferenceElement) parent).getReferenceNameElement()) {
            return highlightReference((RsReferenceElement) parent, holder);
        }
        if (parent instanceof RsMacro) {
            return element == ((RsMacro) parent).getIdentifier() ? RsColor.MACRO : null;
        }
        if (parent instanceof RsMetaVarIdentifier) return RsColor.FUNCTION;
        if (parent instanceof RsMacroBinding) return RsColor.MACRO;
        if (parent instanceof RsNameIdentifierOwner && ((RsNameIdentifierOwner) parent).getNameIdentifier() == element) {
            return colorFor((RsElement) parent);
        }
        return null;
    }

    @Nullable
    private RsColor highlightReference(@NotNull RsReferenceElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof RsPath && ((RsPath) element).getKind() != PathKind.IDENTIFIER) return null;
        if (element instanceof RsPath && RsPathUtil.isInsideDocLink((RsPath) element)) return null;
        if (element instanceof RsExternCrateItem && ((RsExternCrateItem) element).getSelf() != null) return null;

        PsiElement parent = element.getParent();
        boolean isPrimitiveType = element instanceof RsPath && TyPrimitive.fromPath((RsPath) element) != null;

        if (isPrimitiveType) return RsColor.PRIMITIVE_TYPE;
        if (parent instanceof RsMacroCall) {
            return shouldHighlightMacroCall((RsMacroCall) parent, holder) ? RsColor.MACRO : null;
        }
        if (element instanceof RsMethodCall) return RsColor.METHOD_CALL;
        if (element instanceof RsFieldLookup) {
            String text = ((RsFieldLookup) element).getIdentifier() != null ? ((RsFieldLookup) element).getIdentifier().getText() : null;
            if ("await".equals(text) && RsElementUtil.isAtLeastEdition2018(element)) return RsColor.KEYWORD;
        }
        if (element instanceof RsPath && isCall((RsPath) element)) {
            PsiElement ref = element.getReference() != null ? element.getReference().resolve() : null;
            if (ref == null) return null;
            if (ref instanceof RsFunction) {
                RsFunction fn = (RsFunction) ref;
                if (RsFunctionUtil.isAssocFn(fn)) return RsColor.ASSOC_FUNCTION_CALL;
                if (fn.isMethod()) return RsColor.METHOD_CALL;
                return RsColor.FUNCTION_CALL;
            }
            return colorFor((RsElement) ref);
        }
        if (element instanceof RsPath && parent instanceof RsTraitRef) return RsColor.TRAIT;

        PsiElement ref = element.getReference() != null ? element.getReference().resolve() : null;
        if (ref == null) return null;
        return colorFor((RsElement) ref);
    }

    private boolean isCall(@NotNull RsPath path) {
        PsiElement expr = path.getParent() != null ? path.getParent().getParent() : null;
        while (expr instanceof RsParenExpr) {
            expr = expr.getParent();
        }
        return expr instanceof RsCallExpr;
    }

    private boolean shouldHighlightMacroCall(@NotNull RsMacroCall element, @NotNull AnnotationHolder holder) {
        if (!"todo".equals(RsMacroCallUtil.getMacroName(element))) return true;
        return !isTodoHighlightingEnabled(element.getContainingFile(), holder);
    }

    private boolean isTodoHighlightingEnabled(@NotNull PsiFile file, @NotNull AnnotationHolder holder) {
        return OpenApiUtil.getOrPut(holder.getCurrentAnnotationSession(), IS_TODO_HIGHLIGHTING_ENABLED, () -> {
            PsiTodoSearchHelper helper = PsiTodoSearchHelper.getInstance(file.getProject());
            if (!(helper instanceof PsiTodoSearchHelperImpl)) return false;
            if (!((PsiTodoSearchHelperImpl) helper).shouldHighlightInEditor(file)) return false;
            return java.util.Arrays.stream(IndexPatternUtil.getIndexPatterns()).anyMatch(RsTodoSearcher::isTodoPattern);
        });
    }

    @Nullable
    private RsColor macroGroupColor(@NotNull RsElement parent) {
        if (parent instanceof RsMacroExpansionReferenceGroup || parent instanceof RsMacroBindingGroup) return RsColor.MACRO;
        return null;
    }

    @Nullable
    public static RsColor colorFor(@NotNull RsElement element) {
        if (element instanceof RsMacro) return RsColor.MACRO;
        if (element instanceof RsSelfParameter) return RsColor.SELF_PARAMETER;
        if (element instanceof RsEnumItem) return RsColor.ENUM;
        if (element instanceof RsEnumVariant) return RsColor.ENUM_VARIANT;
        if (element instanceof RsExternCrateItem) return RsColor.CRATE;
        if (element instanceof RsConstant) {
            RsConstant constant = (RsConstant) element;
            switch (RsConstantUtil.getKind(constant)) {
                case STATIC: return RsColor.STATIC;
                case MUT_STATIC: return RsColor.MUT_STATIC;
                case CONST: return RsColor.CONSTANT;
            }
        }
        if (element instanceof RsNamedFieldDecl) return RsColor.FIELD;
        if (element instanceof RsFunction) {
            RsFunction fn = (RsFunction) element;
            RsAbstractableOwner owner = fn.getOwner();
            if (owner == RsAbstractableOwner.Foreign || owner == RsAbstractableOwner.Free) return RsColor.FUNCTION;
            if (owner instanceof RsAbstractableOwner.Trait || owner instanceof RsAbstractableOwner.Impl) {
                return RsFunctionUtil.isAssocFn(fn) ? RsColor.ASSOC_FUNCTION : RsColor.METHOD;
            }
        }
        if (element instanceof RsModDeclItem) return RsColor.MODULE;
        if (element instanceof RsMod) {
            return ((RsMod) element).isCrateRoot() ? RsColor.CRATE : RsColor.MODULE;
        }
        if (element instanceof RsPatBinding) {
            return RsElementUtil.ancestorStrict(element, RsValueParameter.class) != null ? RsColor.PARAMETER : RsColor.VARIABLE;
        }
        if (element instanceof RsStructItem) {
            RsStructItem struct = (RsStructItem) element;
            switch (RsStructItemUtil.getKind(struct)) {
                case STRUCT: return RsColor.STRUCT;
                case UNION: return RsColor.UNION;
            }
        }
        if (element instanceof RsTraitItem) return RsColor.TRAIT;
        if (element instanceof RsTypeAlias) return RsColor.TYPE_ALIAS;
        if (element instanceof RsTypeParameter) return RsColor.TYPE_PARAMETER;
        if (element instanceof RsConstParameter) return RsColor.CONST_PARAMETER;
        return null;
    }
}
