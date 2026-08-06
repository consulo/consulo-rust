/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.mir.MirUtils;
import org.rust.lang.core.mir.schemas.MirMatch;
import org.rust.lang.core.mir.schemas.MirBorrowKind;
import org.rust.lang.core.mir.schemas.MirSpan;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.consts.CtUnknown;
import org.rust.lang.core.types.infer.Adjustment;
import org.rust.lang.core.types.infer.RsInferenceResult;
import org.rust.lang.core.types.regions.Scope;
import org.rust.lang.core.types.regions.ScopeTree;
import org.rust.lang.core.types.RvalueScopes;
import org.rust.lang.core.types.regions.RegionScopeTreeUtil;
import org.rust.lang.core.types.ty.*;
import consulo.util.lang.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.rust.lang.core.psi.ext.RsBinaryExprUtil;

public class MirrorContext {
    @Nonnull
    public final ScopeTree regionScopeTree;
    @Nonnull
    private final RsInferenceResult inferenceResult;
    @Nonnull
    private final RvalueScopes rvalueScopes;
    @Nullable
    private Pair<RsExpr, MirSpan> adjustmentSpan;

    public MirrorContext(@Nonnull RsInferenceContextOwner contextOwner) {
        this.regionScopeTree = RegionScopeTreeUtil.getRegionScopeTree(contextOwner);
        this.inferenceResult = ExtensionsUtil.getSelfInferenceResult(contextOwner);
        this.rvalueScopes = RvalueScopes.resolveRvalueScopes(regionScopeTree);
    }

    @Nonnull
    public ThirExpr mirrorBlock(@Nonnull RsBlock block, @Nonnull Ty ty, @Nonnull MirSpan span) {
        ThirExpr mirrored = mirror(block, ty, span);
        return completeMirroring(block, mirrored, Collections.emptyList(), span);
    }

    @Nonnull
    public ThirExpr mirrorBlock(@Nonnull RsBlock block, @Nonnull Ty ty) {
        return mirrorBlock(block, ty, MirUtils.asSpan(block));
    }

    @Nonnull
    public ThirExpr mirrorExpr(@Nonnull RsExpr expr, @Nonnull MirSpan span) {
        ThirExpr mirrored = mirrorUnadjusted(expr, span);
        return completeMirroring(expr, mirrored, ExtensionsUtil.getAdjustments(expr), span);
    }

    @Nonnull
    public ThirExpr mirrorExpr(@Nonnull RsExpr expr) {
        return mirrorExpr(expr, MirUtils.asSpan(expr));
    }

    @Nonnull
    private ThirExpr mirror(@Nonnull RsStructLiteralField field) {
        RsExpr expr = field.getExpr();
        if (expr != null) return mirrorExpr(expr);

        MirSpan span = MirUtils.asSpan(field);
        ThirExpr mirrored = convert((RsElement) field, ExtensionsUtil.getType(field), span);
        return completeMirroring(field, mirrored, ExtensionsUtil.getAdjustments(field), span);
    }

    @Nonnull
    private ThirExpr completeMirroring(
        @Nonnull RsElement element,
        @Nonnull ThirExpr mirrored,
        @Nonnull List<Adjustment> adjustments,
        @Nonnull MirSpan span
    ) {
        MirSpan adjSpan = null;
        if (adjustmentSpan != null && adjustmentSpan.getFirst() == element) {
            adjSpan = adjustmentSpan.getSecond();
        }

        ThirExpr adjusted;
        if (element instanceof RsBreakExpr) {
            adjusted = applyAdjustment(element, mirrored,
                new Adjustment.NeverToAny(TyUnit.INSTANCE),
                adjSpan != null ? adjSpan : mirrored.span);
        } else {
            adjusted = mirrored;
            for (Adjustment adjustment : adjustments) {
                adjusted = applyAdjustment(element, adjusted, adjustment,
                    adjSpan != null ? adjSpan : mirrored.span);
            }
        }

        ThirExpr expr = new ThirExpr.ScopeExpr(
            new Scope.Node((RsElement) span.getReference()), adjusted, adjusted.ty, span
        ).withLifetime(adjusted.getTempLifetime());

        Scope regionScope = regionScopeTree.getDestructionScope((RsElement) span.getReference());
        if (regionScope != null) {
            expr = new ThirExpr.ScopeExpr(regionScope, expr, expr.ty, span)
                .withLifetime(expr.getTempLifetime());
        }

        return expr;
    }

    @Nonnull
    private ThirExpr mirrorUnadjusted(@Nonnull RsExpr expr, @Nonnull MirSpan span) {
        Ty ty = ExtensionsUtil.getType(expr);
        Scope tempLifetime = rvalueScopes.temporaryScope(regionScopeTree, expr);

        ThirExpr result;

        if (expr instanceof RsParenExpr) {
            RsExpr inner = ((RsParenExpr) expr).getExpr();
            if (inner == null) throw new IllegalStateException("Could not get expr of paren expression");
            result = mirrorExpr(inner, span);
        } else if (expr instanceof RsLitExpr) {
            result = new ThirExpr.Literal((RsLitExpr) expr, false, ty, span);
        } else if (expr instanceof RsUnitExpr) {
            result = new ThirExpr.Tuple(Collections.emptyList(), ty, span);
        } else if (expr instanceof RsBlockExpr) {
            ThirBlock block = mirrorBlockInner(((RsBlockExpr) expr).getBlock(), span);
            result = new ThirExpr.Block(block, ty, span);
        } else if (expr instanceof RsBreakExpr) {
            RsBreakExpr breakExpr = (RsBreakExpr) expr;
            RsElement target;
            if (breakExpr.getLabel() != null) {
                target = breakExpr.getLabel().getReference().resolve();
                if (target == null) throw new IllegalStateException("Cannot resolve break target");
            } else {
                target = null;
                for (consulo.language.psi.PsiElement ctx : RsElementUtil.getContexts(breakExpr)) {
                    if (ctx instanceof RsLooplikeExpr) {
                        target = (RsElement) ctx;
                        break;
                    }
                }
                if (target == null) throw new IllegalStateException("Could not find break's loop");
            }
            RsExpr breakValue = breakExpr.getExpr();
            result = new ThirExpr.Break(
                new Scope.Node(target),
                breakValue != null ? mirrorExpr(breakValue) : null,
                ty, span
            );
        } else if (expr instanceof RsPathExpr) {
            result = convertPath((RsPathExpr) expr, ty, span);
        } else if (expr instanceof RsTupleExpr) {
            List<ThirExpr> fields = new ArrayList<>();
            for (RsExpr e : ((RsTupleExpr) expr).getExprList()) {
                fields.add(mirrorExpr(e));
            }
            result = new ThirExpr.Tuple(fields, ty, span);
        } else if (expr instanceof RsMatchExpr) {
            RsMatchExpr matchExpr = (RsMatchExpr) expr;
            RsExpr scrutineeExpr = matchExpr.getExpr();
            if (scrutineeExpr == null) throw new IllegalStateException("match without expr");
            ThirExpr scrutinee = mirrorExpr(scrutineeExpr);
            List<MirMatch.MirArm> arms = new ArrayList<>();
            for (RsMatchArm arm : RsMatchExprUtil.getArms(matchExpr)) {
                arms.add(convertArm(arm));
            }
            result = new ThirExpr.Match(scrutinee, arms, ty, span);
        } else if (expr instanceof RsLetExpr) {
            RsLetExpr letExpr = (RsLetExpr) expr;
            RsPat pat = letExpr.getPat();
            if (pat == null) throw new IllegalStateException("let expr without pattern");
            ThirPat thirPat = ThirPat.from(pat);
            RsExpr initExpr = letExpr.getExpr();
            if (initExpr == null) throw new IllegalStateException("let expr without initializer");
            ThirExpr initializer = mirrorExpr(initExpr);
            result = new ThirExpr.Let(thirPat, initializer, ty, span);
        } else if (expr instanceof RsCastExpr) {
            result = mirrorExprCast(((RsCastExpr) expr).getExpr(), tempLifetime, MirUtils.asSpan(expr), ty, span);
        } else {
            throw new UnsupportedOperationException("Not implemented for " + expr.getClass().getSimpleName());
        }

        result.setTempLifetime(tempLifetime);
        return result;
    }

    @Nonnull
    private ThirExpr convertCond(@Nonnull RsExpr cond) {
        if (cond instanceof RsLetExpr) {
            return mirrorExpr(cond);
        }

        if (cond instanceof RsBinaryExpr && hasLetExpr(cond)) {
            RsBinaryExpr binExpr = (RsBinaryExpr) cond;
            ThirExpr left = convertCond(binExpr.getLeft());
            RsExpr rightExpr = binExpr.getRight();
            if (rightExpr == null) throw new IllegalStateException("Binary expression without right operand");
            ThirExpr right = convertCond(rightExpr);
            return new ThirExpr.Binary(
                RsBinaryExprUtil.getOperatorType(binExpr), left, right,
                ExtensionsUtil.getType(cond), MirUtils.asSpan(cond)
            );
        }

        ThirExpr mirroredExpr = mirrorExpr(cond);
        ThirExpr useExpr = new ThirExpr.Use(mirroredExpr, mirroredExpr.ty, mirroredExpr.span);
        useExpr.setTempLifetime(mirroredExpr.getTempLifetime());
        return useExpr;
    }

    private boolean hasLetExpr(@Nonnull RsExpr expr) {
        if (expr instanceof RsLetExpr) return true;
        if (expr instanceof RsBinaryExpr) {
            RsBinaryExpr binExpr = (RsBinaryExpr) expr;
            if (hasLetExpr(binExpr.getLeft())) return true;
            RsExpr right = binExpr.getRight();
            return right != null && hasLetExpr(right);
        }
        return false;
    }

    @Nonnull
    private ThirExpr methodCallee(@Nonnull RsMethodCall call, @Nonnull MirSpan span, @Nullable Scope tempLifetime) {
        TyFunctionBase methodTy = inferenceResult.getResolvedMethodType(call);
        if (methodTy == null) throw new IllegalStateException("Could not resolve method");
        ThirExpr result = new ThirExpr.ZstLiteral(methodTy, span);
        result.setTempLifetime(tempLifetime);
        return result;
    }

    @Nonnull
    private ThirExpr mirrorExprCast(
        @Nonnull RsExpr sourceExpr,
        @Nullable Scope tempLifetime,
        @Nonnull MirSpan castSpan,
        @Nonnull Ty exprTy,
        @Nonnull MirSpan exprSpan
    ) {
        return new ThirExpr.Cast(mirrorExpr(sourceExpr), exprTy, exprSpan);
    }

    @Nonnull
    private MirMatch.MirArm convertArm(@Nonnull RsMatchArm arm) {
        ThirPat pattern = ThirPat.from(arm.getPat());
        ThirExpr guard = null;
        org.rust.lang.core.psi.RsMatchArmGuard guardPsi = arm.getMatchArmGuard();
        if (guardPsi != null) {
            RsExpr guardExpr = guardPsi.getExpr();
            if (guardExpr != null) guard = mirrorExpr(guardExpr);
        }
        RsExpr bodyExpr = arm.getExpr();
        if (bodyExpr == null) throw new IllegalStateException("match arm without body");
        ThirExpr body = mirrorExpr(bodyExpr);
        return new MirMatch.MirArm(pattern, guard, body, new Scope.Node(arm), MirUtils.asSpan(arm));
    }

    @Nonnull
    private List<FieldExpr> fieldRefs(
        @Nonnull List<RsStructLiteralField> fieldLiterals,
        @Nonnull List<RsFieldDecl> fieldDeclarations
    ) {
        List<FieldExpr> result = new ArrayList<>();
        for (RsStructLiteralField field : fieldLiterals) {
            RsFieldDecl fieldDeclaration = RsStructLiteralFieldUtil.resolveToDeclaration(field);
            if (fieldDeclaration == null) throw new IllegalStateException("Could not resolve RsStructLiteralField");
            int fieldIndex = fieldDeclarations.indexOf(fieldDeclaration);
            if (fieldIndex == -1) throw new IllegalStateException("Can't find RsFieldDecl for RsStructLiteralField");
            result.add(new FieldExpr(fieldIndex, mirror(field)));
        }
        return result;
    }

    @Nonnull
    private ThirExpr convertPath(@Nonnull RsPathExpr path, @Nonnull Ty ty, @Nonnull MirSpan source) {
        RsElement resolved = path.getPath().getReference().resolve();
        if (resolved == null) throw new IllegalStateException("Could not resolve RsPathExpr");
        return convert(resolved, ty, source);
    }

    @Nonnull
    private ThirExpr convert(@Nonnull RsElement resolved, @Nonnull Ty ty, @Nonnull MirSpan source) {
        if (resolved instanceof RsPatBinding) {
            return new ThirExpr.VarRef(new LocalVar.FromPatBinding((RsPatBinding) resolved), ty, source);
        } else if (resolved instanceof RsFieldsOwner) {
            RsFieldsOwner fieldsOwner = (RsFieldsOwner) resolved;
            Pair<RsStructOrEnumItemElement, Integer> pair = getDefinitionAndVariantIndex(fieldsOwner);
            return new ThirExpr.Adt(pair.getFirst(), pair.getSecond(), Collections.emptyList(), null, ty, source);
        } else if (resolved instanceof RsFunction) {
            return new ThirExpr.ZstLiteral(ty, source);
        } else if (resolved instanceof RsConstant) {
            RsConstant constant = (RsConstant) resolved;
            if (constant.getStatic() != null) throw new UnsupportedOperationException("Static constants not yet supported");
            return new ThirExpr.NamedConst(constant, ty, source);
        } else {
            throw new UnsupportedOperationException("Not implemented for " + resolved.getClass().getSimpleName());
        }
    }

    @Nonnull
    private ThirExpr mirror(@Nonnull RsBlock block, @Nonnull Ty ty, @Nonnull MirSpan source) {
        ThirBlock thirBlock = mirrorBlockInner(block, source);
        return new ThirExpr.Block(thirBlock, ty, MirUtils.asSpan(block));
    }

    @Nonnull
    private ThirExpr mirror(@Nonnull RsBlock block, @Nonnull Ty ty) {
        return mirror(block, ty, MirUtils.asSpan(block));
    }

    @Nonnull
    private ThirBlock mirrorBlockInner(@Nonnull RsBlock block, @Nonnull MirSpan source) {
        RsBlockUtil.ExpandedStmtsAndTailExpr expanded = RsBlockUtil.getExpandedStmtsAndTailExpr(block);
        List<RsStmt> stmts = expanded.getStmts();
        RsExpr tailExpr = expanded.getTailExpr();
        return new ThirBlock(
            new Scope.Node((RsElement) source.getReference()),
            regionScopeTree.getDestructionScope(block),
            mirrorStatements(stmts, block),
            tailExpr != null ? mirrorExpr(tailExpr) : null,
            MirUtils.asSpan(block)
        );
    }

    @Nonnull
    private List<ThirStatement> mirrorStatements(@Nonnull List<RsStmt> statements, @Nonnull RsBlock block) {
        List<ThirStatement> result = new ArrayList<>();
        for (RsStmt stmt : statements) {
            Scope destructionScope = regionScopeTree.getDestructionScope(stmt);
            if (stmt instanceof RsLetDecl) {
                RsLetDecl letDecl = (RsLetDecl) stmt;
                Scope.Remainder remainderScope = new Scope.Remainder(block, letDecl);
                RsPat pat = letDecl.getPat();
                if (pat == null) throw new IllegalStateException("Could not find pattern");
                ThirPat pattern = ThirPat.from(pat);
                RsExpr initExpr = letDecl.getExpr();
                result.add(new ThirStatement.Let(
                    remainderScope,
                    new Scope.Node(letDecl),
                    destructionScope,
                    pattern,
                    initExpr != null ? mirrorExpr(initExpr) : null,
                    null
                ));
            } else if (stmt instanceof RsExprStmt) {
                result.add(new ThirStatement.Expr(
                    new Scope.Node(stmt),
                    destructionScope,
                    mirrorExpr(((RsExprStmt) stmt).getExpr())
                ));
            } else {
                throw new UnsupportedOperationException("Not implemented for " + stmt.getClass().getSimpleName());
            }
        }
        return result;
    }

    @Nonnull
    private ThirExpr applyAdjustment(
        @Nonnull RsElement psiExpr,
        @Nonnull ThirExpr thirExpr,
        @Nonnull Adjustment adjustment,
        @Nonnull MirSpan span
    ) {
        ThirExpr result;
        if (adjustment instanceof Adjustment.NeverToAny) {
            result = new ThirExpr.NeverToAny(thirExpr, adjustment.getTarget(), span);
            result.setTempLifetime(thirExpr.getTempLifetime());
        } else if (adjustment instanceof Adjustment.BorrowReference) {
            Adjustment.BorrowReference borrow = (Adjustment.BorrowReference) adjustment;
            MirBorrowKind borrowKind = MirBorrowKind.toBorrowKind(borrow.getMutability());
            result = new ThirExpr.Borrow(borrowKind, thirExpr, adjustment.getTarget(), span);
        } else if (adjustment instanceof Adjustment.Deref) {
            result = new ThirExpr.Deref(thirExpr, adjustment.getTarget(), span);
        } else {
            throw new UnsupportedOperationException("Adjustment not yet supported: " + adjustment.getClass().getSimpleName());
        }
        result.setTempLifetime(thirExpr.getTempLifetime());
        return result;
    }

    @Nonnull
    private static Pair<RsStructOrEnumItemElement, Integer> getDefinitionAndVariantIndex(@Nonnull RsFieldsOwner fieldsOwner) {
        if (fieldsOwner instanceof RsStructItem) {
            return new Pair<>((RsStructOrEnumItemElement) fieldsOwner, 0);
        } else if (fieldsOwner instanceof RsEnumVariant) {
            RsEnumItem enumItem = RsEnumVariantUtil.getParentEnum((RsEnumVariant) fieldsOwner);
            Integer variantIndex = indexOfVariant(enumItem, (RsEnumVariant) fieldsOwner);
            if (variantIndex == null) throw new IllegalStateException("Can't find enum variant");
            return new Pair<>(enumItem, variantIndex);
        } else {
            throw new IllegalStateException("unreachable");
        }
    }

    @Nullable
    public static Integer indexOfVariant(@Nonnull RsEnumItem enumItem, @Nonnull RsEnumVariant variant) {
        int index = RsEnumItemUtil.getVariants(enumItem).indexOf(variant);
        return index != -1 ? index : null;
    }

    @Nonnull
    public static RsFieldsOwner variant(@Nonnull RsStructOrEnumItemElement element, int index) {
        if (element instanceof RsStructItem) {
            return (RsFieldsOwner) element;
        } else if (element instanceof RsEnumItem) {
            return RsEnumItemUtil.getVariants((RsEnumItem) element).get(index);
        } else {
            throw new IllegalStateException("unreachable");
        }
    }

    @Nullable
    public static Integer indexOfField(@Nonnull RsFieldsOwner fieldsOwner, @Nonnull RsFieldDecl field) {
        int index = fieldsOwner.getFields().indexOf(field);
        return index != -1 ? index : null;
    }

    @Nonnull
    public static List<Pair<Integer, Long>> discriminants(@Nonnull RsEnumItem enumItem) {
        List<RsEnumVariant> variants = RsEnumItemUtil.getVariants(enumItem);
        List<Pair<Integer, Long>> result = new ArrayList<>(variants.size());
        for (int i = 0; i < variants.size(); i++) {
            result.add(new Pair<>(i, (long) i));
        }
        return result;
    }
}
