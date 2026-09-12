/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import consulo.language.psi.StubBasedPsiElement;
import consulo.language.psi.stub.StubElement;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.ProcMacroAttribute;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsOuterAttr;
import org.rust.lang.core.stubs.RsAttributeOwnerStub;
import org.rust.lang.core.stubs.RsMetaItemStub;
import org.rust.lang.core.stubs.common.RsAttributeOwnerPsiOrStub;
import org.rust.lang.core.stubs.common.RsMetaItemPsiOrStub;
import org.rust.lang.utils.evaluation.CfgEvaluator;
import org.rust.lang.utils.evaluation.ThreeValuedLogic;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiWhiteSpace;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsInnerAttr;
import org.rust.lang.core.psi.RsTokenSets;
import org.rust.lang.doc.psi.RsDocComment;

public final class RsDocAndAttributeOwnerUtil {

    private RsDocAndAttributeOwnerUtil() {
    }

    @Nonnull
    public static QueryAttributes<RsMetaItem> getQueryAttributes(@Nonnull RsDocAndAttributeOwner owner) {
        return getQueryAttributes(owner, null);
    }

    @Nonnull
    public static QueryAttributes<RsMetaItem> getQueryAttributes(@Nonnull RsDocAndAttributeOwner owner,
                                                                 @Nullable Crate crate) {
        return getQueryAttributes(owner, crate, getAttributeStub(owner), false);
    }

    @Nonnull
    public static QueryAttributes<RsMetaItem> getQueryAttributes(@Nonnull RsDocAndAttributeOwner owner,
                                                                 @Nullable Crate crate,
                                                                 @Nullable RsAttributeOwnerStub stub,
                                                                 boolean outerAttrsOnly) {
        if (stub != null) {
            QueryAttributes<RsMetaItemStub> stubAttrs = getStubQueryAttributes(stub, crate, outerAttrsOnly);
            List<RsMetaItem> psiItems = new ArrayList<>();
            stubAttrs.getMetaItems().forEach(s -> psiItems.add(s.getPsi()));
            return new QueryAttributes<>(psiItems);
        }
        return getExpandedAttributesNoStub(owner, crate, outerAttrsOnly);
    }

    @Nonnull
    private static QueryAttributes<RsMetaItemStub> getStubQueryAttributes(@Nonnull RsAttributeOwnerStub stub,
                                                                          @Nullable Crate crate,
                                                                          boolean outerAttrsOnly) {
        if (!stub.getHasAttrs()) return QueryAttributes.empty();
        if (!stub.getHasCfgAttr()) {
            Stream<RsMetaItemStub> raw = outerAttrsOnly ? stub.getRawOuterMetaItems() : stub.getRawMetaItems();
            return new QueryAttributes<>(raw);
        }
        return getExpandedAttributesNoStub(stub, crate, outerAttrsOnly);
    }

    @Nonnull
    private static <T extends RsMetaItemPsiOrStub> QueryAttributes<T> getExpandedAttributesNoStub(
        @Nonnull RsAttributeOwnerPsiOrStub<T> owner,
        @Nullable Crate explicitCrate,
        boolean outerAttrsOnly
    ) {
        Stream<T> rawMetaItems = outerAttrsOnly ? owner.getRawOuterMetaItems() : owner.getRawMetaItems();
        Crate crate = explicitCrate;
        if (crate == null && owner instanceof RsElement) {
            Crate containing = RsElementUtil.getContainingCrate((RsElement) owner);
            crate = containing == null ? null : Crate.asNotFake(containing);
        }
        if (crate == null) return new QueryAttributes<>(rawMetaItems);
        CfgEvaluator evaluator = CfgEvaluator.forCrate(crate);
        return new QueryAttributes<>(evaluator.expandCfgAttrs(rawMetaItems));
    }

    @Nullable
    public static RsOuterAttr findOuterAttr(@Nonnull RsOuterAttributeOwner owner, @Nonnull String name) {
        for (RsOuterAttr attr : owner.getOuterAttrList()) {
            if (name.equals(RsMetaItemUtil.getName(attr.getMetaItem()))) {
                return attr;
            }
        }
        return null;
    }

    public static boolean isEnabledByCfgSelfOrInAttrProcMacroBody(@Nonnull RsDocAndAttributeOwner owner) {
        return isEnabledByCfgSelfOrInAttrProcMacroBody(owner, null);
    }

    public static boolean isEnabledByCfgSelfOrInAttrProcMacroBody(@Nonnull RsDocAndAttributeOwner owner,
                                                                  @Nullable Crate crate) {
        RsCodeStatus status = CfgUtils.getCodeStatus(owner, crate);
        if (status == RsCodeStatus.ATTR_PROC_MACRO_CALL) return true;
        return evaluateCfg(owner, crate) != ThreeValuedLogic.False;
    }

    public static boolean existsAfterExpansionSelf(@Nonnull RsDocAndAttributeOwner owner) {
        return existsAfterExpansionSelf(owner, null);
    }

    public static boolean existsAfterExpansionSelf(@Nonnull RsDocAndAttributeOwner owner, @Nullable Crate crate) {
        if (evaluateCfg(owner, crate) == ThreeValuedLogic.False) return false;
        if (owner instanceof RsAttrProcMacroOwner) {
            ProcMacroAttribute<RsMetaItem> attr = ProcMacroAttribute.getProcMacroAttribute(
                (RsAttrProcMacroOwner) owner, null, crate, false, false);
            return !(attr instanceof ProcMacroAttribute.Attr);
        }
        return true;
    }

    public static boolean isEnabledByCfgSelf(@Nonnull RsDocAndAttributeOwner owner, @Nullable Crate crate) {
        return evaluateCfg(owner, crate) != ThreeValuedLogic.False;
    }

    public static boolean isCfgUnknownSelf(@Nonnull RsDocAndAttributeOwner owner) {
        return evaluateCfg(owner, null) == ThreeValuedLogic.Unknown;
    }

    @Nonnull
    public static ThreeValuedLogic evaluateCfg(@Nonnull RsAttributeOwnerPsiOrStub<?> owner,
                                               @Nullable Crate crate) {
        // Avoid recursion for RsFile in lazy (resolve-triggering) mode:
        //   RsFile.crate -> RsFile.cachedData -> RsFile.declaration -> RsModDeclItem.resolve ->
        //   -> RsFile.isEnabledByCfg -> RsFile.crate
        if (crate == null && owner instanceof RsFile) return ThreeValuedLogic.True;

        RsAttributeOwnerStub stub = null;
        if (owner instanceof RsDocAndAttributeOwner) {
            stub = getAttributeStub((RsDocAndAttributeOwner) owner);
        } else if (owner instanceof RsAttributeOwnerStub) {
            stub = (RsAttributeOwnerStub) owner;
        }
        if (stub != null && !stub.getMayHaveCfg()) return ThreeValuedLogic.True;

        Crate evalCrate = crate;
        if (evalCrate == null && owner instanceof RsElement) {
            Crate containing = RsElementUtil.getContainingCrate((RsElement) owner);
            evalCrate = containing == null ? null : Crate.asNotFake(containing);
        }
        if (evalCrate == null) return ThreeValuedLogic.True;
        CfgEvaluator evaluator = CfgEvaluator.forCrate(evalCrate);

        Stream<? extends RsMetaItemPsiOrStub> rawMetaItems =
            stub != null ? stub.getRawMetaItems() : owner.getRawMetaItems();
        Stream<? extends RsMetaItemPsiOrStub> expanded =
            (stub != null && !stub.getHasCfgAttr()) ? rawMetaItems : evaluator.expandCfgAttrs(rawMetaItems);

        List<RsMetaItemPsiOrStub> cfgAttrs = new ArrayList<>();
        expanded.forEach(item -> {
            if ("cfg".equals(item.getName())) cfgAttrs.add(item);
        });
        return evaluator.evaluate(cfgAttrs);
    }

    @Nonnull
    public static QueryAttributes<RsMetaItem> getTraversedRawAttributes(@Nonnull RsDocAndAttributeOwner owner,
                                                                        boolean withCfgAttrAttribute) {
        List<RsMetaItem> flat = new ArrayList<>();
        owner.getRawMetaItems().forEach(it -> {
            if ("cfg_attr".equals(RsMetaItemUtil.getName(it))) {
                // First item in cfg_attr is the condition; remaining are the attributes
                java.util.List<RsMetaItem> list = it.getMetaItemArgs() != null
                    ? it.getMetaItemArgs().getMetaItemList()
                    : java.util.Collections.emptyList();
                if (withCfgAttrAttribute) flat.add(it);
                for (int i = 1; i < list.size(); i++) flat.add(list.get(i));
            } else {
                flat.add(it);
            }
        });
        return new QueryAttributes<>(flat);
    }

    @Nullable
    public static RsAttributeOwnerStub getAttributeStub(@Nonnull RsDocAndAttributeOwner owner) {
        StubElement<?> stub = PsiElementUtil.getGreenStubOf(owner);
        return stub instanceof RsAttributeOwnerStub ? (RsAttributeOwnerStub) stub : null;
    }

    /**
     * Returns elements that contribute to documentation of the element.
     */
    @Nonnull
    public static Iterable<PsiElement> docElements(@Nonnull RsDocAndAttributeOwner owner, boolean withInner) {
        List<PsiElement> result = new ArrayList<>();
        for (PsiElement child = owner.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof RsOuterAttr) {
                RsOuterAttr outerAttr = (RsOuterAttr) child;
                if ("doc".equals(RsMetaItemUtil.getName(outerAttr.getMetaItem()))) {
                    result.add(child);
                }
            } else if (child instanceof org.rust.lang.doc.psi.RsDocComment) {
                IElementType tokenType = child.getNode().getElementType();
                if (RsTokenSets.RS_DOC_COMMENTS.contains(tokenType)) {
                    result.add(child);
                }
            } else if (!(child instanceof consulo.language.psi.PsiWhiteSpace)
                && !(child instanceof consulo.language.psi.PsiComment)) {
                break;
            }
        }
        if (withInner) {
            PsiElement childBlock = PsiElementUtil.childOfType(owner, org.rust.lang.core.psi.RsBlock.class);
            PsiElement searchIn = childBlock != null ? childBlock : owner;
            for (PsiElement child = searchIn.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child instanceof org.rust.lang.core.psi.RsInnerAttr) {
                    org.rust.lang.core.psi.RsInnerAttr innerAttr = (org.rust.lang.core.psi.RsInnerAttr) child;
                    if ("doc".equals(RsMetaItemUtil.getName(innerAttr.getMetaItem()))) {
                        result.add(child);
                    }
                } else if (child instanceof org.rust.lang.doc.psi.RsDocComment) {
                    IElementType tokenType = child.getNode().getElementType();
                    if (RsTokenSets.RS_DOC_COMMENTS.contains(tokenType)) {
                        result.add(child);
                    }
                }
            }
        }
        return result;
    }
}
