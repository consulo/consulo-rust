/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.text.SemVer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.util.ToolchainUtil;
import org.rust.ide.fixes.*;
import org.rust.ide.refactoring.RsNamesValidator;
import org.rust.lang.core.CompilerFeature;
import org.rust.lang.core.macros.MacroExpansion;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.ty.Mutability;
import org.rust.lang.core.types.infer.TypeInference;
import org.rust.lang.utils.RsDiagnostic;
import org.rust.stdext.StdextUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.RsLiteralKindUtil;
import org.rust.lang.core.psi.ext.RsStructItemUtil;
import org.rust.lang.core.psi.ext.RsTypeParameterUtil;
import org.rust.lang.core.psi.ext.RsBinaryOpUtil;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.core.psi.ext.RsBinaryExprUtil;
import org.rust.lang.core.psi.ext.RsElement;

public class RsSyntaxErrorsAnnotator extends AnnotatorBase {
    private static final SemVer DEPRECATED_WHERE_CLAUSE_LOCATION_VERSION = ToolchainUtil.parseSemVer("1.61.0");

    @Override
    protected void annotateInternal(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof RsBreakExpr) {
            checkBreakExpr(holder, (RsBreakExpr) element);
        } else if (element instanceof RsContExpr) {
            checkLabelInWhileCondition(holder, (RsContExpr) element);
            checkLabelPointingToBlock(holder, (RsContExpr) element);
        } else if (element instanceof RsExternAbi) {
            checkExternAbi(holder, (RsExternAbi) element);
        } else if (element instanceof RsItemElement) {
            checkItem(holder, (RsItemElement) element);
            if (element instanceof RsFunction) {
                checkFunction(holder, (RsFunction) element);
            } else if (element instanceof RsStructItem) {
                checkStructItem(holder, (RsStructItem) element);
            } else if (element instanceof RsTypeAlias) {
                checkTypeAlias(holder, (RsTypeAlias) element);
            } else if (element instanceof RsImplItem) {
                checkImplItem(holder, (RsImplItem) element);
            } else if (element instanceof RsConstant) {
                checkConstant(holder, (RsConstant) element);
            } else if (element instanceof RsModItem) {
                checkModItem(holder, (RsModItem) element);
            } else if (element instanceof RsModDeclItem) {
                checkModDeclItem(holder, (RsModDeclItem) element);
            } else if (element instanceof RsForeignModItem) {
                checkForeignModItem(holder, (RsForeignModItem) element);
            }
        } else if (element instanceof RsMacro) {
            checkMacro(holder, (RsMacro) element);
        } else if (element instanceof RsMacroCall) {
            checkMacroCall(holder, (RsMacroCall) element);
        } else if (element instanceof RsValueParameterList) {
            checkValueParameterList(holder, (RsValueParameterList) element);
        } else if (element instanceof RsValueParameter) {
            checkValueParameter(holder, (RsValueParameter) element);
        } else if (element instanceof RsTypeParameterList) {
            checkTypeParameterList(holder, (RsTypeParameterList) element);
        } else if (element instanceof RsTypeParameter) {
            checkTypeParameter(holder, (RsTypeParameter) element);
        } else if (element instanceof RsTypeArgumentList) {
            checkTypeArgumentList(holder, (RsTypeArgumentList) element);
        } else if (element instanceof RsLetExpr) {
            checkLetExpr(holder, (RsLetExpr) element);
        } else if (element instanceof RsPat) {
            checkPat(holder, (RsPat) element);
        } else if (element instanceof RsTraitType) {
            checkTraitType(holder, (RsTraitType) element);
        } else if (element instanceof RsUnderscoreExpr) {
            checkUnderscoreExpr(holder, (RsUnderscoreExpr) element);
        } else if (element instanceof RsWherePred) {
            checkWherePred(holder, (RsWherePred) element);
        } else if (element instanceof RsLambdaExpr) {
            checkLambdaExpr(holder, (RsLambdaExpr) element);
        } else if (element instanceof RsDefaultParameterValue) {
            checkDefaultParameterValue(holder, (RsDefaultParameterValue) element);
        } else if (element instanceof RsTypeParamBounds) {
            checkTypeParamBounds(holder, (RsTypeParamBounds) element);
        } else if (element instanceof RsSuperStructs) {
            checkSuperStructs(holder, (RsSuperStructs) element);
        } else if (element instanceof RsPrefixIncExpr || element instanceof RsPostfixIncExpr || element instanceof RsPostfixDecExpr) {
            checkIncDecOp(holder, (RsExpr) element);
        } else {
            checkReservedKeyword(holder, element);
        }
    }

    private static void checkBreakExpr(@NotNull AnnotationHolder holder, @NotNull RsBreakExpr item) {
        checkLabelInWhileCondition(holder, item);
        if (item.getExpr() == null) return;
        RsLabel label = item.getLabel();
        PsiElement loop;
        if (label != null) {
            PsiElement labelBlock = label.getReference().resolve();
            if (labelBlock == null) return;
            loop = labelBlock.getParent();
        } else {
            loop = PsiTreeUtil.getParentOfType(item, RsForExpr.class, RsWhileExpr.class, RsLoopExpr.class, RsItemElement.class);
        }
        if (loop instanceof RsForExpr) {
            RsDiagnostic.addToHolder(new RsDiagnostic.BreakExprInNonLoopError(item, "for"), holder);
        } else if (loop instanceof RsWhileExpr) {
            RsDiagnostic.addToHolder(new RsDiagnostic.BreakExprInNonLoopError(item, "while"), holder);
        }
    }

    private static void checkLabelInWhileCondition(@NotNull AnnotationHolder holder, @NotNull RsLabelReferenceOwner item) {
        if (item.getLabel() != null) return;
        RsCondition condition = PsiTreeUtil.getParentOfType(
            (PsiElement) item, RsCondition.class, true, RsLooplikeExpr.class, RsItemElement.class
        );
        if (condition == null) return;
        if (condition.getParent() instanceof RsWhileExpr) {
            List<RsAddLabelFix> fixes = !holder.isBatchMode()
                ? Collections.singletonList(new RsAddLabelFix(item))
                : Collections.emptyList();
            RsDiagnostic.addToHolder(new RsDiagnostic.BreakContinueInWhileConditionWithoutLoopError(
                (PsiElement) item, ((PsiElement) item).getText(), fixes
            ), holder);
        }
    }

    private static void checkLabelPointingToBlock(@NotNull AnnotationHolder holder, @NotNull RsContExpr contExpr) {
        RsLabel label = contExpr.getLabel();
        if (label == null) return;
        List<? extends PsiElement> resolved = label.getReference().multiResolve();
        List<RsBlockExpr> blocks = new ArrayList<>();
        for (PsiElement r : resolved) {
            if (r.getParent() instanceof RsBlockExpr) blocks.add((RsBlockExpr) r.getParent());
        }
        if (!blocks.isEmpty()) {
            List<RsConvertBlockToLoopFix> fixes = blocks.size() == 1
                ? Collections.singletonList(new RsConvertBlockToLoopFix(blocks.get(0)))
                : Collections.emptyList();
            RsDiagnostic.addToHolder(new RsDiagnostic.ContinueLabelTargetBlock(contExpr, fixes), holder);
        }
    }

    private static void checkItem(@NotNull AnnotationHolder holder, @NotNull RsItemElement item) {
        checkItemOrMacro(item, StdextUtil.pluralize(item.getItemKindName()).substring(0, 1).toUpperCase() + StdextUtil.pluralize(item.getItemKindName()).substring(1), RsItemElementUtil.getItemDefKeyword(item), holder);
    }

    private static void checkMacro(@NotNull AnnotationHolder holder, @NotNull RsMacro element) {
        checkItemOrMacro(element, "Macros", RsMacroUtil.getMacroRules(element), holder);
    }

    private static void checkItemOrMacro(@NotNull RsElement item, @NotNull String itemName, @NotNull PsiElement highlightElement, @NotNull AnnotationHolder holder) {
        if (!(item instanceof RsAbstractable)) {
            PsiElement parent = item.getContext();
            PsiElement owner = parent instanceof RsMembers ? ((RsMembers) parent).getContext() : parent;
            if (owner instanceof RsItemElement && (owner instanceof RsForeignModItem || owner instanceof RsTraitOrImpl)) {
                RsItemElement ownerItem = (RsItemElement) owner;
                holder.newAnnotation(HighlightSeverity.ERROR,
                    RsBundle.message("inspection.message.are.not.allowed.inside", itemName, RsItemElementUtil.getArticle(ownerItem), ownerItem.getItemKindName()))
                    .range(highlightElement).create();
            }
        }
        if (!(item instanceof RsAbstractable) && !(item instanceof RsTraitOrImpl)) {
            denyDefaultKeyword(item, holder, itemName);
        }
    }

    private static void denyDefaultKeyword(@NotNull RsElement item, @NotNull AnnotationHolder holder, @NotNull String itemName) {
        PsiElement defaultKw = item.getNode().findChildByType(RsElementTypes.DEFAULT) != null
            ? item.getNode().findChildByType(RsElementTypes.DEFAULT).getPsi() : null;
        deny(defaultKw, holder, RsBundle.message("inspection.message.cannot.have.default.qualifier11", itemName));
    }

    private static void checkMacroCall(@NotNull AnnotationHolder holder, @NotNull RsMacroCall element) {
        denyDefaultKeyword(element, holder, "Macro invocations");
    }

    private static void checkFunction(@NotNull AnnotationHolder holder, @NotNull RsFunction fn) {
        // Simplified function check
    }

    private static void checkStructItem(@NotNull AnnotationHolder holder, @NotNull RsStructItem struct) {
        if (RsStructItemUtil.getKind(struct) == RsStructKind.UNION && struct.getTupleFields() != null) {
            deny(struct.getTupleFields(), holder, RsBundle.message("inspection.message.union.cannot.be.tuple.like"));
        }
    }

    private static void checkSuperStructs(@NotNull AnnotationHolder holder, @NotNull RsSuperStructs element) {
        deny(element, holder, RsBundle.message("error.message.struct.inheritance.is.not.supported"),
            HighlightSeverity.ERROR, new RemoveElementFix(element, "super structs"));
    }

    private static void checkTypeAlias(@NotNull AnnotationHolder holder, @NotNull RsTypeAlias ta) {
        // Simplified type alias check
    }

    private static void checkConstant(@NotNull AnnotationHolder holder, @NotNull RsConstant constant) {
        // Simplified constant check
    }

    private static void checkValueParameterList(@NotNull AnnotationHolder holder, @NotNull RsValueParameterList params) {
        // Simplified check
    }

    private static void checkValueParameter(@NotNull AnnotationHolder holder, @NotNull RsValueParameter param) {
        // Simplified check
    }

    private static void checkTypeParameterList(@NotNull AnnotationHolder holder, @NotNull RsTypeParameterList element) {
        // Simplified check
    }

    private static void checkTypeParameter(@NotNull AnnotationHolder holder, @NotNull RsTypeParameter item) {
        if (RsTypeParameterUtil.getBounds(item).stream().filter(b -> RsPolyboundUtil.getHasQ(b)).count() > 1) {
            RsDiagnostic.addToHolder(new RsDiagnostic.MultipleRelaxedBoundsError(item), holder);
        }
    }

    private static void checkTypeArgumentList(@NotNull AnnotationHolder holder, @NotNull RsTypeArgumentList args) {
        // Simplified check
    }

    private static void checkImplItem(@NotNull AnnotationHolder holder, @NotNull RsImplItem item) {
        PsiElement unsafe = item.getUnsafe();
        if (unsafe != null && item.getTraitRef() == null) {
            RsTypeReference typeReference = item.getTypeReference();
            if (typeReference == null) return;
            RsDiagnostic.addToHolder(new RsDiagnostic.UnsafeInherentImplError(
                typeReference, Collections.singletonList(new RemoveElementFix(unsafe))
            ), holder);
        }
    }

    private static void checkExternAbi(@NotNull AnnotationHolder holder, @NotNull RsExternAbi element) {
        RsLitExpr litExpr = element.getLitExpr();
        if (litExpr == null) return;
        RsLiteralKind kind = RsLiteralKindUtil.getKind(litExpr);
        if (kind == null) return;
        if (!(kind instanceof RsLiteralKind.StringLiteral)) {
            holder.newAnnotation(HighlightSeverity.ERROR, RsBundle.message("inspection.message.non.string.abi.literal"))
                .range(litExpr).create();
        }
    }

    private static void checkModDeclItem(@NotNull AnnotationHolder holder, @NotNull RsModDeclItem element) {
        checkInvalidUnsafe(holder, element.getUnsafe(), "Module");
    }

    private static void checkModItem(@NotNull AnnotationHolder holder, @NotNull RsModItem element) {
        checkInvalidUnsafe(holder, element.getUnsafe(), "Module");
    }

    private static void checkForeignModItem(@NotNull AnnotationHolder holder, @NotNull RsForeignModItem element) {
        checkInvalidUnsafe(holder, element.getUnsafe(), "Extern block");
    }

    private static void checkInvalidUnsafe(@NotNull AnnotationHolder holder, @Nullable PsiElement unsafe, @NotNull String itemName) {
        if (unsafe != null) {
            holder.newAnnotation(HighlightSeverity.ERROR, RsBundle.message("inspection.message.cannot.be.declared.unsafe", itemName))
                .range(unsafe).create();
        }
    }

    private static void checkLetExpr(@NotNull AnnotationHolder holder, @NotNull RsLetExpr element) {
        PsiElement ancestor = element.getParent();
        while (true) {
            if (ancestor instanceof RsCondition || ancestor instanceof RsMatchArmGuard) return;
            if (ancestor instanceof RsBinaryExpr && ((RsBinaryExpr) ancestor).getBinaryOp().getAndand() != null) {
                ancestor = ancestor.getParent();
                continue;
            }
            break;
        }
        deny((PsiElement) element, holder, RsBundle.message("inspection.message.let.expressions.are.not.supported.here"));
    }

    private static void checkPat(@NotNull AnnotationHolder holder, @NotNull RsPat element) {
        if (element instanceof RsPatRange) {
            checkPatRange(holder, (RsPatRange) element);
        }
    }

    private static void checkPatRange(@NotNull AnnotationHolder holder, @NotNull RsPatRange element) {
        // Simplified check
    }

    private static void checkTraitType(@NotNull AnnotationHolder holder, @NotNull RsTraitType element) {
        // Simplified check
    }

    private static void checkUnderscoreExpr(@NotNull AnnotationHolder holder, @NotNull RsUnderscoreExpr element) {
        boolean isAllowed = false;
        RsBinaryExpr binaryExpr = RsElementUtil.ancestorStrict(element, RsBinaryExpr.class);
        if (binaryExpr != null && RsBinaryOpUtil.getOperatorType(binaryExpr) instanceof AssignmentOp && RsPsiJavaUtil.isAncestorOf(binaryExpr.getLeft(), element)) {
            isAllowed = true;
        }
        if (!isAllowed) {
            deny((PsiElement) element, holder, RsBundle.message("inspection.message.in.expressions.can.only.be.used.on.left.hand.side.assignment"));
        }
    }

    private static void checkWherePred(@NotNull AnnotationHolder holder, @NotNull RsWherePred boundPred) {
        // Simplified check
    }

    private static void checkLambdaExpr(@NotNull AnnotationHolder holder, @NotNull RsLambdaExpr lambda) {
        PsiElement asyncElement = RsLambdaExprUtil.getAsync(lambda);
        if (asyncElement != null && lambda.getMove() == null) {
            RsValueParameterList valueParameterList = lambda.getValueParameterList();
            if (!valueParameterList.getValueParameterList().isEmpty()) {
                RsDiagnostic.addToHolder(new RsDiagnostic.AsyncNonMoveClosureWithParameters(asyncElement, valueParameterList), holder);
            }
        }
    }

    private static void checkDefaultParameterValue(@NotNull AnnotationHolder holder, @NotNull RsDefaultParameterValue defaultValue) {
        RemoveElementFix fix = new RemoveElementFix(defaultValue, "default parameter value");
        deny(defaultValue.getExpr(), holder, RsBundle.message("inspection.message.default.parameter.values.are.not.supported.in.rust"),
            HighlightSeverity.ERROR, fix);
    }

    private static void checkTypeParamBounds(@NotNull AnnotationHolder holder, @NotNull RsTypeParamBounds bounds) {
        PsiElement impl = bounds.getImpl();
        if (impl != null) {
            RemoveElementFix fix = new RemoveElementFix(impl, "`impl` keyword");
            deny(impl, holder, RsBundle.message("inspection.message.expected.trait.bound.found.impl.trait.type"),
                HighlightSeverity.ERROR, fix);
        }
        PsiElement dyn = RsTypeParamBoundsUtil.getDyn(bounds);
        if (dyn != null) {
            RemoveElementFix fix = new RemoveElementFix(dyn, "`dyn` keyword");
            deny(dyn, holder, RsBundle.message("inspection.message.invalid.dyn.keyword"),
                HighlightSeverity.ERROR, fix);
        }
    }

    private static void checkIncDecOp(@NotNull AnnotationHolder holder, @NotNull RsExpr expr) {
        PsiElement operator;
        if (expr instanceof RsPrefixIncExpr) {
            operator = ((RsPrefixIncExpr) expr).getInc();
        } else if (expr instanceof RsPostfixIncExpr) {
            operator = ((RsPostfixIncExpr) expr).getInc();
        } else if (expr instanceof RsPostfixDecExpr) {
            operator = ((RsPostfixDecExpr) expr).getDec();
        } else {
            return;
        }
        RsDiagnostic.addToHolder(new RsDiagnostic.RustHasNoIncDecOperator(operator), holder);
    }

    private static void checkReservedKeyword(@NotNull AnnotationHolder holder, @NotNull PsiElement item) {
        if (RsPsiJavaUtil.elementType(item) == RsElementTypes.IDENTIFIER
            && RsNamesValidator.RESERVED_KEYWORDS.contains(item.getText())) {
            PsiElement macroRelatedParent = PsiTreeUtil.getParentOfType(
                item, RsMacroArgument.class, RsMacroExpansionContents.class, RsMacroPatternContents.class,
                RsMetaItemArgs.class, RsCompactTT.class
            );
            if (macroRelatedParent != null) return;

            PsiElement parent = item.getParent();
            List<LocalQuickFix> fixes = new ArrayList<>();
            if (parent instanceof RsNameIdentifierOwner && ((RsNameIdentifierOwner) parent).getNameIdentifier() == item) {
                fixes.add(new EscapeKeywordFix(item, false));
            }
            RsDiagnostic.addToHolder(new RsDiagnostic.ReservedIdentifierIsUsed(item, fixes), holder);
        }
    }

    private static void deny(
        @Nullable PsiElement el,
        @NotNull AnnotationHolder holder,
        @NotNull String message
    ) {
        deny(el, holder, message, HighlightSeverity.ERROR, null);
    }

    private static void deny(
        @Nullable PsiElement el,
        @NotNull AnnotationHolder holder,
        @NotNull String message,
        @NotNull HighlightSeverity severity,
        @Nullable IntentionAction fix
    ) {
        if (el == null) return;
        com.intellij.lang.annotation.AnnotationBuilder builder = holder.newAnnotation(severity, message).range(el.getTextRange());
        if (fix != null) builder.withFix(fix);
        builder.create();
    }
}
