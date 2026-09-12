/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.lang.SemVer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import consulo.language.editor.annotation.AnnotationBuilder;
import consulo.language.editor.inspection.ProblemDescriptor;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.RustcInfo;
import org.rust.cargo.toolchain.impl.RustcVersion;
import org.rust.lang.core.types.RsTypesUtil;

public class RsSyntaxErrorsAnnotator extends AnnotatorBase {
    private static final SemVer DEPRECATED_WHERE_CLAUSE_LOCATION_VERSION = ToolchainUtil.parseSemVer("1.61.0");

    @Override
    protected void annotateInternal(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
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

    private static void checkBreakExpr(@Nonnull AnnotationHolder holder, @Nonnull RsBreakExpr item) {
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

    private static void checkLabelInWhileCondition(@Nonnull AnnotationHolder holder, @Nonnull RsLabelReferenceOwner item) {
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

    private static void checkLabelPointingToBlock(@Nonnull AnnotationHolder holder, @Nonnull RsContExpr contExpr) {
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

    private static void checkItem(@Nonnull AnnotationHolder holder, @Nonnull RsItemElement item) {
        checkItemOrMacro(item, StdextUtil.pluralize(item.getItemKindName()).substring(0, 1).toUpperCase() + StdextUtil.pluralize(item.getItemKindName()).substring(1), RsItemElementUtil.getItemDefKeyword(item), holder);
    }

    private static void checkMacro(@Nonnull AnnotationHolder holder, @Nonnull RsMacro element) {
        checkItemOrMacro(element, "Macros", RsMacroUtil.getMacroRules(element), holder);
    }

    private static void checkItemOrMacro(@Nonnull RsElement item, @Nonnull String itemName, @Nonnull PsiElement highlightElement, @Nonnull AnnotationHolder holder) {
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

    private static void denyDefaultKeyword(@Nonnull RsElement item, @Nonnull AnnotationHolder holder, @Nonnull String itemName) {
        PsiElement defaultKw = item.getNode().findChildByType(RsElementTypes.DEFAULT) != null
            ? item.getNode().findChildByType(RsElementTypes.DEFAULT).getPsi() : null;
        deny(defaultKw, holder, RsBundle.message("inspection.message.cannot.have.default.qualifier11", itemName));
    }

    private static void checkMacroCall(@Nonnull AnnotationHolder holder, @Nonnull RsMacroCall element) {
        denyDefaultKeyword(element, holder, "Macro invocations");
    }

    private static void checkFunction(@Nonnull AnnotationHolder holder, @Nonnull RsFunction fn) {
        String title = RsFunctionUtil.getTitle(fn);
        RsAbstractableOwner owner = RsFunctionUtil.getOwner(fn);
        RsBlock block = RsFunctionUtil.getBlock(fn);
        PsiElement defaultKw = RsFunctionUtil.getDefault(fn);
        if (owner instanceof RsAbstractableOwner.Trait) {
            deny(defaultKw, holder, RsBundle.message("inspection.message.cannot.have.default.qualifier9", title));
            deny(fn.getVis(), holder, RsBundle.message("inspection.message.cannot.have.pub.qualifier2", title));
        } else if (owner instanceof RsAbstractableOwner.Impl) {
            require(block, holder, RsBundle.message("inspection.message.must.have.body", title), fn.getLastChild());
            if (defaultKw != null) {
                deny(fn.getVis(), holder, RsBundle.message("inspection.message.default.cannot.have.pub.qualifier", firstLower(title)));
            }
        } else if (owner == RsAbstractableOwner.Foreign) {
            deny(defaultKw, holder, RsBundle.message("inspection.message.cannot.have.default.qualifier8", title));
            deny(block, holder, RsBundle.message("inspection.message.cannot.have.body2", title));
            deny(fn.getConst(), holder, RsBundle.message("inspection.message.cannot.have.const.qualifier", title));
            deny(fn.getUnsafe(), holder, RsBundle.message("inspection.message.cannot.have.unsafe.qualifier", title));
            deny(fn.getExternAbi(), holder, RsBundle.message("inspection.message.cannot.have.extern.abi", title));
        } else {
            require(block, holder, RsBundle.message("inspection.message.must.have.body2", title), fn.getLastChild());
            deny(defaultKw, holder, RsBundle.message("inspection.message.cannot.have.default.qualifier10", title));
        }
    }

    private static void checkStructItem(@Nonnull AnnotationHolder holder, @Nonnull RsStructItem struct) {
        if (RsStructItemUtil.getKind(struct) == RsStructKind.UNION && struct.getTupleFields() != null) {
            deny(struct.getTupleFields(), holder, RsBundle.message("inspection.message.union.cannot.be.tuple.like"));
        }
    }

    private static void checkSuperStructs(@Nonnull AnnotationHolder holder, @Nonnull RsSuperStructs element) {
        deny(element, holder, RsBundle.message("error.message.struct.inheritance.is.not.supported"),
            HighlightSeverity.ERROR, new RemoveElementFix(element, "super structs"));
    }

    private static void checkTypeAlias(@Nonnull AnnotationHolder holder, @Nonnull RsTypeAlias ta) {
        String title = RsBundle.message("inspection.message.type.0", ta.getIdentifier().getText());

        PsiElement eq = ta.getEq();
        List<RsWhereClause> whereClauses = ta.getWhereClauseList();
        RsWhereClause whereClauseBeforeEq = null;
        RsWhereClause whereClauseAfterEq = null;
        if (eq != null && !whereClauses.isEmpty()) {
            RsWhereClause first = whereClauses.get(0);
            if (first.getTextRange().getStartOffset() < eq.getTextRange().getStartOffset()) {
                whereClauseBeforeEq = first;
            }
            RsWhereClause last = whereClauses.get(whereClauses.size() - 1);
            if (last.getTextRange().getStartOffset() > eq.getTextRange().getStartOffset()) {
                whereClauseAfterEq = last;
            }
        }

        PsiElement defaultKw = RsTypeAliasUtil.getDefault(ta);
        RsAbstractableOwner owner = ta.getOwner();
        if (owner instanceof RsAbstractableOwner.Trait) {
            deny(defaultKw, holder, RsBundle.message("inspection.message.cannot.have.default.qualifier6", title));
        } else if (owner instanceof RsAbstractableOwner.Impl) {
            if (((RsAbstractableOwner.Impl) owner).isInherent()) {
                deny(defaultKw, holder, RsBundle.message("inspection.message.cannot.have.default.qualifier5", title));
            }
            deny(ta.getTypeParamBounds(), holder, RsBundle.message("inspection.message.bounds.on.have.no.effect2", title));
            require(ta.getTypeReference(), holder, RsBundle.message("inspection.message.should.have.body", title), ta);

            CargoProject cargoProject = RsElementExtUtil.getCargoProject(ta);
            RustcInfo rustcInfo = cargoProject != null ? cargoProject.getRustcInfo() : null;
            RustcVersion rustcVersion = rustcInfo != null ? rustcInfo.getVersion() : null;
            SemVer version = rustcVersion != null ? rustcVersion.getSemver() : null;
            if (version == null || version.compareTo(DEPRECATED_WHERE_CLAUSE_LOCATION_VERSION) < 0) return;
            deny(whereClauseBeforeEq, holder,
                RsBundle.message("inspection.message.cannot.have.where.clause.before.type", title),
                HighlightSeverity.WEAK_WARNING, null);
        } else if (owner == RsAbstractableOwner.Foreign) {
            deny(defaultKw, holder, RsBundle.message("inspection.message.cannot.have.default.qualifier4", title));
            deny(ta.getTypeParameterList(), holder, RsBundle.message("inspection.message.cannot.have.generic.parameters", title));
            deny(whereClauses.isEmpty() ? null : whereClauses.get(0), holder,
                RsBundle.message("inspection.message.cannot.have.where.clause", title));
            deny(ta.getTypeParamBounds(), holder, RsBundle.message("inspection.message.bounds.on.have.no.effect", title));
            denyRange(ta.getTypeReference(), holder, RsBundle.message("inspection.message.cannot.have.body", title), ta);
        } else {
            deny(defaultKw, holder, RsBundle.message("inspection.message.cannot.have.default.qualifier7", title));
            deny(ta.getTypeParamBounds(), holder, RsBundle.message("inspection.message.bounds.on.have.no.effect3", title));
            require(ta.getTypeReference(), holder, RsBundle.message("inspection.message.should.have.body2", title), ta);
            deny(whereClauseAfterEq, holder, RsBundle.message("inspection.message.cannot.have.where.clause.after.type", title));
        }
    }

    private static void checkConstant(@Nonnull AnnotationHolder holder, @Nonnull RsConstant constant) {
        String name = RsConstantUtil.getNameLikeElement(constant).getText();
        String title = constant.getStatic() != null
            ? RsBundle.message("inspection.message.static.constant", name)
            : RsBundle.message("inspection.message.constant", name);
        PsiElement defaultKw = RsConstantUtil.getDefault(constant);
        RsAbstractableOwner owner = constant.getOwner();
        if (owner == RsAbstractableOwner.Foreign) {
            deny(defaultKw, holder, RsBundle.message("inspection.message.cannot.have.default.qualifier2", title));
            require(constant.getStatic(), holder,
                RsBundle.message("inspection.message.only.static.constants.are.allowed.in.extern.blocks"), constant.getConst());
            denyRange(constant.getExpr(), holder,
                RsBundle.message("inspection.message.static.constants.in.extern.blocks.cannot.have.values"),
                constant.getEq(), constant.getExpr());
        } else if (owner instanceof RsAbstractableOwner.Trait) {
            deny(constant.getVis(), holder, RsBundle.message("inspection.message.cannot.have.pub.qualifier", title));
            deny(defaultKw, holder, RsBundle.message("inspection.message.cannot.have.default.qualifier", title));
            deny(constant.getStatic(), holder, RsBundle.message("inspection.message.static.constants.are.not.allowed.in.traits"));
        } else if (owner instanceof RsAbstractableOwner.Impl) {
            deny(constant.getStatic(), holder, RsBundle.message("inspection.message.static.constants.are.not.allowed.in.impl.blocks"));
            require(constant.getExpr(), holder, RsBundle.message("inspection.message.must.have.value", title), constant);
        } else {
            deny(defaultKw, holder, RsBundle.message("inspection.message.cannot.have.default.qualifier3", title));
            require(constant.getExpr(), holder, RsBundle.message("inspection.message.must.have.value2", title), constant);
        }
        checkConstantType(holder, constant);
    }

    private static void checkConstantType(@Nonnull AnnotationHolder holder, @Nonnull RsConstant element) {
        if (element.getColon() != null || element.getTypeReference() != null) return;
        PsiElement nameElement = RsConstantUtil.getNameLikeElement(element);
        String typeText = RsConstantUtil.isConst(element) ? "const" : "static";
        String message = RsBundle.message("inspection.message.missing.type.for.item", typeText);

        AnnotationBuilder annotation =
            holder.newAnnotation(HighlightSeverity.ERROR, message).range(nameElement);

        RsExpr expr = element.getExpr();
        if (expr != null) {
            annotation = annotation.withFix(new AddTypeFix(nameElement, RsTypesUtil.getType(expr)));
        }

        annotation.create();
    }

    private static void checkValueParameterList(@Nonnull AnnotationHolder holder, @Nonnull RsValueParameterList params) {
        if (!(params.getParent() instanceof RsFunction fn)) return;
        RsVariadic variadic = params.getVariadic();
        PsiElement dot3 = variadic != null ? variadic.getDotdotdot() : null;
        RsAbstractableOwner owner = RsFunctionUtil.getOwner(fn);
        if (owner instanceof RsAbstractableOwner.Trait || owner instanceof RsAbstractableOwner.Impl) {
            deny(dot3, holder, RsBundle.message("inspection.message.cannot.be.variadic2", RsFunctionUtil.getTitle(fn)));
        } else if (owner == RsAbstractableOwner.Foreign) {
            deny(params.getSelfParameter(), holder,
                RsBundle.message("inspection.message.cannot.have.self.parameter", RsFunctionUtil.getTitle(fn)));
            checkDot3Parameter(holder, dot3);
        } else {
            deny(params.getSelfParameter(), holder,
                RsBundle.message("inspection.message.cannot.have.self.parameter2", RsFunctionUtil.getTitle(fn)));
            checkVariadic(holder, fn, dot3);
        }
    }

    private static void checkVariadic(@Nonnull AnnotationHolder holder, @Nonnull RsFunction fn, @Nullable PsiElement dot3) {
        if (dot3 == null) return;
        if (fn.getUnsafe() != null && "C".equals(RsFunctionUtil.getActualAbiName(fn))) {
            CompilerFeature.getC_VARIADIC().check(holder, dot3, "C-variadic functions");
        } else {
            deny(dot3, holder, RsBundle.message("inspection.message.cannot.be.variadic", RsFunctionUtil.getTitle(fn)));
        }
    }

    private static void checkDot3Parameter(@Nonnull AnnotationHolder holder, @Nullable PsiElement dot3) {
        if (dot3 == null) return;
        PsiElement next = PsiTreeUtil.nextVisibleLeaf(dot3);
        if (next == null) return;
        if (!")".equals(next.getText())) {
            holder.newAnnotation(HighlightSeverity.ERROR,
                    RsBundle.message("inspection.message.must.be.last.in.argument.list.for.variadic.function"))
                .range(next).create();
        }
    }

    private static void checkValueParameter(@Nonnull AnnotationHolder holder, @Nonnull RsValueParameter param) {
        PsiElement grandParent = param.getParent() != null ? param.getParent().getParent() : null;
        if (grandParent instanceof RsFunction fn) {
            checkValueParameterInFunction(fn, param, holder);
        }
    }

    private static void checkValueParameterInFunction(@Nonnull RsFunction fn, @Nonnull RsValueParameter param, @Nonnull AnnotationHolder holder) {
        RsPat pat = param.getPat();
        RsAbstractableOwner owner = RsFunctionUtil.getOwner(fn);
        if (owner == RsAbstractableOwner.Foreign) {
            require(pat, holder,
                RsBundle.message("inspection.message.cannot.have.anonymous.parameters", RsFunctionUtil.getTitle(fn)), param);
        } else if (owner instanceof RsAbstractableOwner.Trait) {
            if (pat == null) {
                String message = RsBundle.message("inspection.message.anonymous.functions.parameters.are.deprecated.rfc");
                SubstituteTextFix fix = SubstituteTextFix.replace(
                    RsBundle.message("intention.name.add.dummy.parameter.name"),
                    param.getContainingFile(),
                    param.getTextRange(),
                    "_: " + param.getText()
                );
                ProblemDescriptor descriptor =
                    InspectionManager.getInstance(param.getProject())
                        .createProblemDescriptor(param, message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, true);
                holder.newAnnotation(HighlightSeverity.WARNING, message)
                    .newLocalQuickFix(fix, descriptor).registerFix().create();
            }
        } else {
            require(pat, holder,
                RsBundle.message("inspection.message.cannot.have.anonymous.parameters2", RsFunctionUtil.getTitle(fn)), param);
        }
    }

    private static void checkTypeParameterList(@Nonnull AnnotationHolder holder, @Nonnull RsTypeParameterList element) {
        PsiElement parent = element.getParent();
        List<RsTypeParameter> typeParameters = element.getTypeParameterList();
        if (parent instanceof RsImplItem || parent instanceof RsFunction) {
            for (RsTypeParameter p : typeParameters) {
                RsTypeReference defaultValue = p.getTypeReference();
                if (defaultValue != null) {
                    holder.newAnnotation(HighlightSeverity.ERROR, RsBundle.message(
                            "inspection.message.defaults.for.type.parameters.are.only.allowed.in.struct.enum.type.or.trait.definitions"))
                        .range(defaultValue).create();
                }
            }
        } else {
            int lastNotDefaultIndex = 0;
            for (int i = 0; i < typeParameters.size(); i++) {
                if (typeParameters.get(i).getTypeReference() == null) lastNotDefaultIndex = i;
            }
            for (int i = 0; i < lastNotDefaultIndex && i < typeParameters.size(); i++) {
                RsTypeParameter p = typeParameters.get(i);
                if (p.getTypeReference() != null) {
                    holder.newAnnotation(HighlightSeverity.ERROR,
                            RsBundle.message("inspection.message.type.parameters.with.default.must.be.trailing"))
                        .range(p).create();
                }
            }
        }

        checkTypeList(element, "parameters", holder);
    }

    private static void checkTypeParameter(@Nonnull AnnotationHolder holder, @Nonnull RsTypeParameter item) {
        if (RsTypeParameterUtil.getBounds(item).stream().filter(b -> RsPolyboundUtil.getHasQ(b)).count() > 1) {
            RsDiagnostic.addToHolder(new RsDiagnostic.MultipleRelaxedBoundsError(item), holder);
        }
    }

    private static void checkTypeArgumentList(@Nonnull AnnotationHolder holder, @Nonnull RsTypeArgumentList args) {
        checkTypeList(args, "arguments", holder);

        List<RsAssocTypeBinding> bindings = args.getAssocTypeBindingList();
        if (bindings.isEmpty()) return;
        int startOfAssocTypeBindings = bindings.get(0).getTextOffset();

        List<PsiElement> generics = new ArrayList<>();
        generics.addAll(args.getLifetimeList());
        generics.addAll(args.getTypeReferenceList());
        generics.addAll(args.getExprList());
        for (PsiElement generic : generics) {
            if (generic.getTextOffset() > startOfAssocTypeBindings) {
                holder.newAnnotation(HighlightSeverity.ERROR,
                        RsBundle.message("inspection.message.generic.arguments.must.come.before.first.constraint"))
                    .range(generic).create();
            }
        }
    }

    private static void checkTypeList(@Nonnull PsiElement typeList, @Nonnull String elementsName, @Nonnull AnnotationHolder holder) {
        TypeKind kind = TypeKind.LIFETIME;
        for (PsiElement child = typeList.getFirstChild(); child != null; child = child.getNextSibling()) {
            TypeKind newKind = TypeKind.forType(child);
            if (newKind == null) continue;
            if (newKind.canStandAfter(kind)) {
                kind = newKind;
            } else {
                String newStateName = newKind.getPresentableName();
                newStateName = newStateName.substring(0, 1).toUpperCase() + newStateName.substring(1);
                holder.newAnnotation(HighlightSeverity.ERROR, RsBundle.message(
                        "inspection.message.must.be.declared.prior.to", newStateName, elementsName, kind.getPresentableName(), elementsName))
                    .range(child).create();
            }
        }
    }

    private enum TypeKind {
        LIFETIME,
        TYPE,
        CONST;

        String getPresentableName() {
            return name().toLowerCase();
        }

        boolean canStandAfter(@Nonnull TypeKind prev) {
            return this != LIFETIME || prev == LIFETIME;
        }

        @Nullable
        static TypeKind forType(@Nonnull PsiElement seekingElement) {
            if (seekingElement instanceof RsLifetimeParameter || seekingElement instanceof RsLifetime) return LIFETIME;
            if (seekingElement instanceof RsTypeParameter || seekingElement instanceof RsTypeReference) return TYPE;
            if (seekingElement instanceof RsConstParameter || seekingElement instanceof RsExpr) return CONST;
            return null;
        }
    }

    private static void checkImplItem(@Nonnull AnnotationHolder holder, @Nonnull RsImplItem item) {
        PsiElement unsafe = item.getUnsafe();
        if (unsafe != null && item.getTraitRef() == null) {
            RsTypeReference typeReference = item.getTypeReference();
            if (typeReference == null) return;
            RsDiagnostic.addToHolder(new RsDiagnostic.UnsafeInherentImplError(
                typeReference, Collections.singletonList(new RemoveElementFix(unsafe))
            ), holder);
        }
    }

    private static void checkExternAbi(@Nonnull AnnotationHolder holder, @Nonnull RsExternAbi element) {
        RsLitExpr litExpr = element.getLitExpr();
        if (litExpr == null) return;
        RsLiteralKind kind = RsLiteralKindUtil.getKind(litExpr);
        if (kind == null) return;
        if (!(kind instanceof RsLiteralKind.StringLiteral)) {
            holder.newAnnotation(HighlightSeverity.ERROR, RsBundle.message("inspection.message.non.string.abi.literal"))
                .range(litExpr).create();
        }
    }

    private static void checkModDeclItem(@Nonnull AnnotationHolder holder, @Nonnull RsModDeclItem element) {
        checkInvalidUnsafe(holder, element.getUnsafe(), "Module");
    }

    private static void checkModItem(@Nonnull AnnotationHolder holder, @Nonnull RsModItem element) {
        checkInvalidUnsafe(holder, element.getUnsafe(), "Module");
    }

    private static void checkForeignModItem(@Nonnull AnnotationHolder holder, @Nonnull RsForeignModItem element) {
        checkInvalidUnsafe(holder, element.getUnsafe(), "Extern block");
    }

    private static void checkInvalidUnsafe(@Nonnull AnnotationHolder holder, @Nullable PsiElement unsafe, @Nonnull String itemName) {
        if (unsafe != null) {
            holder.newAnnotation(HighlightSeverity.ERROR, RsBundle.message("inspection.message.cannot.be.declared.unsafe", itemName))
                .range(unsafe).create();
        }
    }

    private static void checkLetExpr(@Nonnull AnnotationHolder holder, @Nonnull RsLetExpr element) {
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

    private static void checkPat(@Nonnull AnnotationHolder holder, @Nonnull RsPat element) {
        if (element instanceof RsPatRange) {
            checkPatRange(holder, (RsPatRange) element);
        }
    }

    private static void checkPatRange(@Nonnull AnnotationHolder holder, @Nonnull RsPatRange element) {
        RsPatConst start = RsPatRangeUtil.getStart(element);
        RsPatConst end = RsPatRangeUtil.getEnd(element);
        if (element.getDotdot() != null) {
            if (start == null && end == null) {
                deny(element.getDotdot(), holder, RsBundle.message("inspection.message.unexpected3"));
            }
        } else if (element.getDotdoteq() != null) {
            if (start == null && end == null) {
                deny(element.getDotdoteq(), holder, RsBundle.message("inspection.message.unexpected2"));
            }
        } else if (element.getDotdotdot() != null) {
            if (start == null && end == null) {
                deny(element.getDotdotdot(), holder, RsBundle.message("inspection.message.unexpected"));
            } else if (start == null) {
                deny(element.getDotdotdot(), holder, RsBundle.message("inspection.message.range.to.patterns.with.are.not.allowed"));
            }
        }
    }

    private static void checkTraitType(@Nonnull AnnotationHolder holder, @Nonnull RsTraitType element) {
        // Simplified check
    }

    private static void checkUnderscoreExpr(@Nonnull AnnotationHolder holder, @Nonnull RsUnderscoreExpr element) {
        boolean isAllowed = false;
        RsBinaryExpr binaryExpr = RsElementUtil.ancestorStrict(element, RsBinaryExpr.class);
        if (binaryExpr != null && RsBinaryOpUtil.getOperatorType(binaryExpr) instanceof AssignmentOp && RsPsiJavaUtil.isAncestorOf(binaryExpr.getLeft(), element)) {
            isAllowed = true;
        }
        if (!isAllowed) {
            deny((PsiElement) element, holder, RsBundle.message("inspection.message.in.expressions.can.only.be.used.on.left.hand.side.assignment"));
        }
    }

    private static void checkWherePred(@Nonnull AnnotationHolder holder, @Nonnull RsWherePred boundPred) {
        // Simplified check
    }

    private static void checkLambdaExpr(@Nonnull AnnotationHolder holder, @Nonnull RsLambdaExpr lambda) {
        PsiElement asyncElement = RsLambdaExprUtil.getAsync(lambda);
        if (asyncElement != null && lambda.getMove() == null) {
            RsValueParameterList valueParameterList = lambda.getValueParameterList();
            if (!valueParameterList.getValueParameterList().isEmpty()) {
                RsDiagnostic.addToHolder(new RsDiagnostic.AsyncNonMoveClosureWithParameters(asyncElement, valueParameterList), holder);
            }
        }
    }

    private static void checkDefaultParameterValue(@Nonnull AnnotationHolder holder, @Nonnull RsDefaultParameterValue defaultValue) {
        RemoveElementFix fix = new RemoveElementFix(defaultValue, "default parameter value");
        deny(defaultValue.getExpr(), holder, RsBundle.message("inspection.message.default.parameter.values.are.not.supported.in.rust"),
            HighlightSeverity.ERROR, fix);
    }

    private static void checkTypeParamBounds(@Nonnull AnnotationHolder holder, @Nonnull RsTypeParamBounds bounds) {
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

    private static void checkIncDecOp(@Nonnull AnnotationHolder holder, @Nonnull RsExpr expr) {
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

    private static void checkReservedKeyword(@Nonnull AnnotationHolder holder, @Nonnull PsiElement item) {
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
        @Nonnull AnnotationHolder holder,
        @Nonnull String message
    ) {
        deny(el, holder, message, HighlightSeverity.ERROR, null);
    }

    /** Reports {@code message} over {@code highlightElements} when {@code el} is absent. */
    private static void require(
        @Nullable PsiElement el,
        @Nonnull AnnotationHolder holder,
        @Nonnull String message,
        @Nullable PsiElement... highlightElements
    ) {
        if (el != null) return;
        TextRange range = combinedRange(highlightElements);
        if (range == null) return;
        holder.newAnnotation(HighlightSeverity.ERROR, message).range(range).create();
    }

    /** Reports {@code message} over {@code highlightElements} when {@code el} is present. */
    private static void denyRange(
        @Nullable PsiElement el,
        @Nonnull AnnotationHolder holder,
        @Nonnull String message,
        @Nullable PsiElement... highlightElements
    ) {
        if (el == null) return;
        TextRange range = combinedRange(highlightElements);
        holder.newAnnotation(HighlightSeverity.ERROR, message)
            .range(range != null ? range : el.getTextRange())
            .create();
    }

    @Nullable
    private static TextRange combinedRange(@Nullable PsiElement... elements) {
        TextRange result = null;
        if (elements == null) return null;
        for (PsiElement element : elements) {
            if (element == null) continue;
            result = result == null ? element.getTextRange() : result.union(element.getTextRange());
        }
        return result;
    }

    @Nonnull
    private static String firstLower(@Nonnull String text) {
        return text.isEmpty() ? text : Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }

    private static void deny(
        @Nullable PsiElement el,
        @Nonnull AnnotationHolder holder,
        @Nonnull String message,
        @Nonnull HighlightSeverity severity,
        @Nullable IntentionAction fix
    ) {
        if (el == null) return;
        consulo.language.editor.annotation.AnnotationBuilder builder = holder.newAnnotation(severity, message).range(el.getTextRange());
        if (fix != null) builder.withFix(fix);
        builder.create();
    }
}
