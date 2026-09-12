/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.util.dataholder.Key;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsMetaItemUtil;
import org.rust.lang.core.stubs.RsPathStub;

public final class RsExpandedElementUtil {
    private RsExpandedElementUtil() {}

    public static final Key<RangeMap> RS_FORCED_REDUCED_RANGE_MAP_KEY =
        Key.create("org.rust.lang.core.psi.RS_FORCED_REDUCED_RANGE_MAP");

    /** @deprecated Use RS_FORCED_REDUCED_RANGE_MAP_KEY */
    private static final Key<RangeMap> RS_FORCED_REDUCED_RANGE_MAP = RS_FORCED_REDUCED_RANGE_MAP_KEY;

    public static void setContext(@Nonnull RsExpandedElement element, @Nonnull RsElement context) {
        RsFile containingFile = element.getContainingFile() instanceof RsFile ? (RsFile) element.getContainingFile() : null;
        if (containingFile != null) {
            setRsFileContext(containingFile, context, false);
        }
        setExpandedElementContext(element, context);
    }

    /** Internal. Use {@link #setContext}. */
    public static void setExpandedElementContext(@Nonnull RsExpandedElement element, @Nonnull RsElement context) {
        element.putUserData(RsExpandedElement.RS_EXPANSION_CONTEXT, context);
    }

    /** Internal. Use {@link #setContext}. */
    public static void setRsFileContext(@Nonnull RsFile file, @Nonnull RsElement context, boolean isInMemoryMacroExpansion) {
        RsFile contextContainingFile = RsElementUtil.getContainingRsFileSkippingCodeFragments(context);
        if (contextContainingFile != null) {
            file.inheritCachedDataFrom(contextContainingFile, isInMemoryMacroExpansion);
        }
    }

    public static void setForcedReducedRangeMap(@Nonnull RsFile file, @Nonnull RangeMap ranges) {
        file.putUserData(RS_FORCED_REDUCED_RANGE_MAP, ranges);
    }

    @Nullable
    public static RsPossibleMacroCall getExpandedFrom(@Nonnull RsExpandedElement element) {
        return MacroExpansionManagerUtil.getMacroExpansionManager(element.getProject()).getExpandedFrom(element);
    }

    @Nullable
    public static RsPossibleMacroCall getExpandedFromRecursively(@Nonnull RsExpandedElement element) {
        RsPossibleMacroCall call = getExpandedFrom(element);
        if (call == null) return null;
        while (true) {
            RsPossibleMacroCall parent = getExpandedFrom((RsExpandedElement) call);
            if (parent == null) break;
            call = parent;
        }
        return call;
    }

    @Nullable
    public static RsMacroCall getIncludedFrom(@Nonnull PsiElement element) {
        PsiElement containingFile = RsElementUtil.getStubParent(element);
        if (!(containingFile instanceof RsFile)) return null;
        RsFile rsFile = (RsFile) containingFile;
        if (rsFile.isIncludedByIncludeMacro()) {
            return MacroExpansionManagerUtil.getMacroExpansionManager(rsFile.getProject()).getIncludedFrom(rsFile);
        }
        return null;
    }

    @Nullable
    public static RsPossibleMacroCall getExpandedOrIncludedFrom(@Nonnull RsExpandedElement element) {
        RsPossibleMacroCall expandedFrom = getExpandedFrom(element);
        if (expandedFrom != null) return expandedFrom;
        return getIncludedFrom(element);
    }

    @Nullable
    public static RsPossibleMacroCall findMacroCallExpandedFrom(@Nonnull PsiElement element) {
        RsPossibleMacroCall found = findMacroCallExpandedFromNonRecursive(element);
        if (found == null) return null;
        RsPossibleMacroCall recursive = findMacroCallExpandedFrom(found);
        return recursive != null ? recursive : found;
    }

    @Nullable
    public static RsPossibleMacroCall findMacroCallExpandedFromNonRecursive(@Nonnull PsiElement element) {
        PsiElement current = element;
        while (current != null) {
            current = RsElementUtil.getStubParent(current);
            if (current instanceof RsExpandedElement) {
                RsPossibleMacroCall from = getExpandedFrom((RsExpandedElement) current);
                if (from != null) return from;
            }
        }
        return null;
    }

    public static boolean isExpandedFromMacro(@Nonnull PsiElement element) {
        return findMacroCallExpandedFromNonRecursive(element) != null;
    }

    public static boolean isExpandedFromIncludeMacro(@Nonnull PsiElement element) {
        PsiElement containingFile = RsElementUtil.getStubParent(element);
        if (!(containingFile instanceof RsFile)) return false;
        return ((RsFile) containingFile).isIncludedByIncludeMacro();
    }

    @Nullable
    public static PsiElement findElementExpandedFrom(@Nonnull PsiElement element, boolean strict) {
        PsiElement expandedFrom = findElementExpandedFromUnchecked(element);
        if (strict) {
            return expandedFrom != null && !isExpandedFromMacro(expandedFrom) ? expandedFrom : null;
        }
        return expandedFrom;
    }

    @Nullable
    public static PsiElement findElementExpandedFrom(@Nonnull PsiElement element) {
        return findElementExpandedFrom(element, true);
    }

    @Nullable
    private static PsiElement findElementExpandedFromUnchecked(@Nonnull PsiElement element) {
        MacroCallAndOffset callAndOffset = findMacroCallAndOffsetExpandedFromUnchecked(
            element, element.getTextOffset(), RangeMap.StickTo.RIGHT
        );
        if (callAndOffset == null) return null;
        PsiElement found = callAndOffset.getCall().getContainingFile().findElementAt(callAndOffset.getAbsoluteOffset());
        if (found != null && found.getTextOffset() == callAndOffset.getAbsoluteOffset()) {
            return found;
        }
        return null;
    }

    @Nullable
    public static MacroCallAndOffset findMacroCallAndOffsetExpandedFromUnchecked(
        @Nonnull PsiElement anchor,
        int startOffset,
        @Nonnull RangeMap.StickTo stickTo
    ) {
        RangeMap ranges = anchor.getContainingFile().getUserData(RS_FORCED_REDUCED_RANGE_MAP);
        if (ranges != null) {
            Integer mappedOffset = ranges.mapOffsetFromExpansionToCallBody(startOffset, stickTo);
            if (mappedOffset == null) return null;
            RsPossibleMacroCall call = findMacroCallExpandedFrom(anchor);
            if (call == null) return null;
            Integer absolute = fromBodyRelativeOffset(mappedOffset, call);
            if (absolute == null) return null;
            return new MacroCallAndOffset(call, absolute);
        }
        MacroCallAndOffset mappedElement = findMacroCallAndOffsetExpandedFromNonRecursive(anchor, startOffset, stickTo);
        if (mappedElement == null) return null;
        MacroCallAndOffset recursive = findMacroCallAndOffsetExpandedFromUnchecked(
            mappedElement.getCall(), mappedElement.getAbsoluteOffset(), stickTo
        );
        return recursive != null ? recursive : mappedElement;
    }

    @Nullable
    private static MacroCallAndOffset findMacroCallAndOffsetExpandedFromNonRecursive(
        @Nonnull PsiElement anchor,
        int startOffset,
        @Nonnull RangeMap.StickTo stickTo
    ) {
        RsPossibleMacroCall call = findMacroCallExpandedFromNonRecursive(anchor);
        if (call == null) return null;
        Integer mappedOffset = mapOffsetFromExpansionToCallBody(call, startOffset, stickTo);
        if (mappedOffset == null) return null;
        return new MacroCallAndOffset(call, mappedOffset);
    }

    @Nullable
    private static Integer mapOffsetFromExpansionToCallBody(
        @Nonnull RsPossibleMacroCall call,
        int offset,
        @Nonnull RangeMap.StickTo stickTo
    ) {
        Integer relative = mapOffsetFromExpansionToCallBodyRelative(call, offset, stickTo);
        if (relative == null) return null;
        return fromBodyRelativeOffset(relative, call);
    }

    @Nullable
    private static Integer mapOffsetFromExpansionToCallBodyRelative(
        @Nonnull RsPossibleMacroCall call,
        int offset,
        @Nonnull RangeMap.StickTo stickTo
    ) {
        MacroExpansion expansion = RsPossibleMacroCallUtil.getExpansion(call);
        if (expansion == null) return null;
        int fileOffset = MacroExpansionUtil.getExpansionContext(call).getExpansionFileStartOffset();
        return MacroExpansionUtil.getRanges(expansion).mapOffsetFromExpansionToCallBody(offset - fileOffset, stickTo);
    }

    public static boolean cameFromMacroCall(@Nonnull PsiElement element) {
        RsPossibleMacroCall call = findMacroCallExpandedFromNonRecursive(element);
        if (!(call instanceof RsMacroCall)) return false;
        int startOffset = element.getTextOffset();
        if (element instanceof RsPath) {
            Object greenStub = ((RsPath) element).getStub();
            if (greenStub != null) {
                startOffset = ((org.rust.lang.core.stubs.RsPathStub) greenStub).getStartOffset();
            }
        }
        return mapOffsetFromExpansionToCallBodyRelative(call, startOffset, RangeMap.StickTo.RIGHT) != null;
    }

    @Nullable
    public static RsPossibleMacroCall findMacroCallFromWhichLeafIsExpanded(@Nonnull PsiElement element) {
        int startOffset = element.getTextOffset();
        if (element instanceof RsPath) {
            Object greenStub = ((RsPath) element).getStub();
            if (greenStub != null) {
                startOffset = ((org.rust.lang.core.stubs.RsPathStub) greenStub).getStartOffset();
            }
        }
        MacroCallAndOffset result = findMacroCallAndOffsetExpandedFromUnchecked(element, startOffset, RangeMap.StickTo.RIGHT);
        return result != null ? result.getCall() : null;
    }

    @Nullable
    public static List<PsiElement> findExpansionElements(@Nonnull PsiElement element) {
        List<PsiElement> mappedElements = findExpansionElementsNonRecursive(element);
        if (mappedElements == null) return null;
        List<PsiElement> result = new ArrayList<>();
        for (PsiElement mapped : mappedElements) {
            List<PsiElement> expanded = findExpansionElements(mapped);
            if (expanded != null && !expanded.isEmpty()) {
                result.addAll(expanded);
            } else {
                result.add(mapped);
            }
        }
        return result;
    }

    @Nonnull
    public static PsiElement findExpansionElementOrSelf(@Nonnull PsiElement element) {
        List<PsiElement> result = findExpansionElements(element);
        if (result != null && result.size() == 1) return result.get(0);
        return element;
    }

    @Nullable
    private static List<PsiElement> findExpansionElementsNonRecursive(@Nonnull PsiElement element) {
        List<PsiElement> ancestors = new ArrayList<>();
        PsiElement current = element;
        while (current != null) {
            ancestors.add(current);
            current = current.getParent();
        }

        RsPossibleMacroCall call = null;
        for (int i = ancestors.size() - 1; i >= 0; i--) {
            PsiElement it = ancestors.get(i);
            if (it instanceof RsMacroArgument) {
                call = RsElementUtil.ancestorStrict(it, RsMacroCall.class);
                if (call != null) break;
            }
            if (it instanceof RsAttrProcMacroOwner) {
                call = AttrCache.NoCache.INSTANCE.cachedGetProcMacroAttribute((RsAttrProcMacroOwner) it);
                if (call != null) break;
            }
        }
        if (call == null) return null;

        if (call instanceof RsMetaItem) {
            RsPath path = ((RsMetaItem) call).getPath();
            for (PsiElement ancestor : ancestors) {
                if (ancestor == path) return null;
                if (ancestor instanceof RsMetaItem && org.rust.lang.core.psi.ext.RsMetaItemUtil.isRootMetaItem((RsMetaItem) ancestor) && "cfg".equals(((RsMetaItem) ancestor).getName())) {
                    return null;
                }
            }
        }

        MacroExpansion expansion = RsPossibleMacroCallUtil.getExpansion(call);
        if (expansion == null) return Collections.emptyList();
        List<Integer> mappedOffsets = mapOffsetFromCallBodyToExpansion(call, expansion, element.getTextOffset());
        if (mappedOffsets == null) return Collections.emptyList();
        RsFile expansionFile = expansion.getFile();
        List<PsiElement> result = new ArrayList<>();
        for (Integer mappedOffset : mappedOffsets) {
            PsiElement found = expansionFile.findElementAt(mappedOffset);
            if (found != null && found.getTextOffset() == mappedOffset) {
                result.add(found);
            }
        }
        return result;
    }

    @Nullable
    private static List<Integer> mapOffsetFromCallBodyToExpansion(
        @Nonnull RsPossibleMacroCall call,
        @Nonnull MacroExpansion expansion,
        int absOffsetInCallBody
    ) {
        Integer relOffset = toBodyRelativeOffset(absOffsetInCallBody, call);
        if (relOffset == null) return null;
        int fileOffset = MacroExpansionUtil.getExpansionContext(call).getExpansionFileStartOffset();
        List<Integer> offsets = MacroExpansionUtil.getRanges(expansion).mapOffsetFromCallBodyToExpansion(relOffset);
        List<Integer> result = new ArrayList<>(offsets.size());
        for (Integer offset : offsets) {
            result.add(offset + fileOffset);
        }
        return result;
    }

    @Nullable
    private static Integer toBodyRelativeOffset(int offset, @Nonnull RsPossibleMacroCall call) {
        TextRange bodyTextRange = RsPossibleMacroCallUtil.getBodyTextRange(call);
        if (bodyTextRange == null) return null;
        if (!bodyTextRange.contains(offset)) return null;
        int macroOffset = bodyTextRange.getStartOffset();
        int elementOffset = offset - macroOffset;
        if (elementOffset < 0) throw new IllegalStateException("elementOffset < 0");
        return elementOffset;
    }

    @Nullable
    private static Integer fromBodyRelativeOffset(int offset, @Nonnull RsPossibleMacroCall call) {
        TextRange macroRange = RsPossibleMacroCallUtil.getBodyTextRange(call);
        if (macroRange == null) return null;
        int elementOffset = offset + macroRange.getStartOffset();
        if (elementOffset > macroRange.getEndOffset()) throw new IllegalStateException("elementOffset > macroRange.endOffset");
        return elementOffset;
    }

    /**
     * The range inside the macro call body that the given range of the expansion came from, or {@code null}
     * if it did not come from a single contiguous range of the same length.
     */
    @Nullable
    public static TextRange mapRangeFromExpansionToCallBodyStrict(
        @Nonnull RsPossibleMacroCall call,
        @Nonnull TextRange range
    ) {
        List<TextRange> mapped = mapRangeFromExpansionToCallBody(call, range);
        if (mapped.size() == 1 && mapped.get(0).getLength() == range.getLength()) {
            return mapped.get(0);
        }
        return null;
    }

    @Nonnull
    public static List<TextRange> mapRangeFromExpansionToCallBody(
        @Nonnull RsPossibleMacroCall call,
        @Nonnull TextRange range
    ) {
        MacroExpansion expansion = RsPossibleMacroCallUtil.getExpansion(call);
        if (expansion == null) return Collections.emptyList();
        MappedTextRange mappedRange = new MappedTextRange(range.getStartOffset(), range.getStartOffset(), range.getLength());
        List<MappedTextRange> mapped = mapRangeFromExpansionToCallBody(expansion, call, mappedRange);
        List<TextRange> result = new ArrayList<>(mapped.size());
        for (MappedTextRange m : mapped) {
            result.add(m.getSrcRange());
        }
        return result;
    }

    @Nonnull
    private static List<MappedTextRange> mapRangeFromExpansionToCallBody(
        @Nonnull MacroExpansion expansion,
        @Nonnull RsPossibleMacroCall call,
        @Nonnull MappedTextRange range
    ) {
        int fileOffset = MacroExpansionUtil.getExpansionContext(call).getExpansionFileStartOffset();
        if (range.getSrcOffset() - fileOffset < 0) return Collections.emptyList();
        List<MappedTextRange> mappedRanges = MacroExpansionUtil.getRanges(expansion)
            .mapMappedTextRangeFromExpansionToCallBody(range.srcShiftLeft(fileOffset));
        List<MappedTextRange> result = new ArrayList<>();
        for (MappedTextRange mr : mappedRanges) {
            Integer newSrcOffset = fromBodyRelativeOffset(mr.getSrcOffset(), call);
            if (newSrcOffset != null) {
                result.add(new MappedTextRange(newSrcOffset, mr.getDstOffset(), mr.getLength()));
            }
        }
        RsPossibleMacroCall parentCall = findMacroCallExpandedFromNonRecursive(call);
        if (parentCall == null) return result;
        List<MappedTextRange> finalResult = new ArrayList<>();
        for (MappedTextRange mr : result) {
            MacroExpansion parentExpansion = RsPossibleMacroCallUtil.getExpansion(parentCall);
            if (parentExpansion == null) return Collections.emptyList();
            finalResult.addAll(mapRangeFromExpansionToCallBody(parentExpansion, parentCall, mr));
        }
        return finalResult;
    }

    @Nullable
    public static TextRange mapRangeFromExpansionToCallBodyStrict(
        @Nonnull PsiElement anchor,
        @Nonnull TextRange range
    ) {
        RangeMap ranges = anchor.getContainingFile().getUserData(RS_FORCED_REDUCED_RANGE_MAP);
        if (ranges != null) {
            List<MappedTextRange> mapped = ranges.mapTextRangeFromExpansionToCallBody(range);
            if (mapped.size() == 1 && mapped.get(0).getSrcRange().getLength() == range.getLength()) {
                return mapped.get(0).getSrcRange();
            }
            return null;
        }
        RsPossibleMacroCall call = findMacroCallExpandedFromNonRecursive(anchor);
        if (call == null) return null;
        List<TextRange> mapped = mapRangeFromExpansionToCallBody(call, range);
        if (mapped.size() == 1 && mapped.get(0).getLength() == range.getLength()) {
            return mapped.get(0);
        }
        return null;
    }

    @Nullable
    public static PsiElement findNavigationTargetIfMacroExpansion(@Nonnull PsiElement element) {
        PsiElement target = element;
        if (element instanceof RsNameIdentifierOwner) {
            PsiElement nameId = ((RsNameIdentifierOwner) element).getNameIdentifier();
            if (nameId != null) target = nameId;
        }
        PsiElement expanded = findElementExpandedFrom(target);
        if (expanded != null) return expanded;
        RsPossibleMacroCall call = findMacroCallExpandedFrom(element);
        if (call != null) return RsPossibleMacroCallUtil.getPath(call);
        return null;
    }

    /**
     * For a {@link RsMacroCall}, the context is inferred from its PSI parent; for a meta-item-based
     * call, it is always {@link MacroExpansionContext#ITEM}.
     */
    @Nonnull
    public static MacroExpansionContext getExpansionContext(@Nonnull RsPossibleMacroCall call) {
        RsPossibleMacroCallKind kind = RsPossibleMacroCallUtil.getKind(call);
        if (kind instanceof RsPossibleMacroCallKind.MetaItem) return MacroExpansionContext.ITEM;
        if (kind instanceof RsPossibleMacroCallKind.MacroCall) {
            PsiElement context = ((RsPossibleMacroCallKind.MacroCall) kind).call.getContext();
            if (context instanceof RsMacroExpr) return MacroExpansionContext.EXPR;
            if (context instanceof RsBlock) return MacroExpansionContext.STMT;
            if (context instanceof RsPatMacro) return MacroExpansionContext.PAT;
            if (context instanceof RsMacroType) return MacroExpansionContext.TYPE;
            if (context instanceof RsMetaItem) return MacroExpansionContext.META_ITEM_VALUE;
        }
        return MacroExpansionContext.ITEM;
    }

    /** Convenience overload that builds the factory from {@code project}. */
    @Nullable
    public static MacroExpansion parseExpandedTextWithContext(
        @Nonnull MacroExpansionContext context,
        @Nonnull consulo.project.Project project,
        @Nonnull CharSequence text
    ) {
        return parseExpandedTextWithContext(context, new org.rust.lang.core.psi.RsPsiFactory(project), text);
    }

    /**
     * Wraps the expanded text in a syntactic frame matching the given context, parses it and
     * unwraps the relevant element back out via {@link #getExpansionFromExpandedFile}.
     */
    @Nullable
    public static MacroExpansion parseExpandedTextWithContext(
        @Nonnull MacroExpansionContext context,
        @Nonnull org.rust.lang.core.psi.RsPsiFactory factory,
        @Nonnull CharSequence text
    ) {
        consulo.language.psi.PsiFile psiFile = factory.createPsiFile(context.prepareExpandedTextForParsing(text));
        if (!(psiFile instanceof RsFile)) return null;
        return getExpansionFromExpandedFile(context, (RsFile) psiFile);
    }

    /**
     * extracts the expanded elements back out of the syntactic frame added by
     * {@link MacroExpansionContext#prepareExpandedTextForParsing}.
     */
    @Nullable
    public static MacroExpansion getExpansionFromExpandedFile(
        @Nonnull MacroExpansionContext context,
        @Nonnull RsFile file
    ) {
        switch (context) {
            case EXPR: {
                RsExpr expr = RsElementUtil.stubDescendantOfTypeOrStrict(file, RsExpr.class);
                return expr != null ? new MacroExpansion.Expr(file, expr) : null;
            }
            case PAT: {
                RsPat pat = RsElementUtil.stubDescendantOfTypeOrStrict(file, RsPat.class);
                return pat != null ? new MacroExpansion.Pat(file, pat) : null;
            }
            case TYPE: {
                RsTypeReference type = RsElementUtil.stubDescendantOfTypeOrStrict(file, RsTypeReference.class);
                return type != null ? new MacroExpansion.Type(file, type) : null;
            }
            case STMT: {
                RsBlock block = RsElementUtil.stubDescendantOfTypeOrStrict(file, RsBlock.class);
                if (block == null) return null;
                List<RsExpandedElement> elements = PsiElementUtil.stubChildrenOfType(block, RsExpandedElement.class);
                return new MacroExpansion.Stmts(file, elements);
            }
            case META_ITEM_VALUE: {
                RsAttr attr = RsElementUtil.stubDescendantOfTypeOrStrict(file, RsAttr.class);
                if (attr == null) return null;
                RsMetaItem metaItem = attr.getMetaItem();
                consulo.language.psi.PsiElement value = metaItem.getLitExpr();
                if (value == null) {
                    value = PsiElementUtil.childOfType(metaItem, RsMacroCall.class);
                }
                if (!(value instanceof RsExpandedElement)) return null;
                return new MacroExpansion.MetaItemValue(file, (RsExpandedElement) value);
            }
            case ITEM:
            default: {
                List<RsExpandedElement> items = PsiElementUtil.stubChildrenOfType(file, RsExpandedElement.class);
                return new MacroExpansion.Items(file, items);
            }
        }
    }

    @Nullable
    public static MacroExpansion getExpansionFromExpandedFile(
        @Nonnull MacroExpansionContext context,
        @Nonnull consulo.language.psi.PsiFile file
    ) {
        if (file instanceof RsFile) {
            return getExpansionFromExpandedFile(context, (RsFile) file);
        }
        return null;
    }

    @Nullable
    public static PsiElement expandedFromRecursively(@Nonnull PsiElement element) {
        PsiElement current = element;
        while (true) {
            PsiElement expanded = findElementExpandedFrom(current);
            if (expanded == null) return current == element ? null : current;
            current = expanded;
        }
    }
}
