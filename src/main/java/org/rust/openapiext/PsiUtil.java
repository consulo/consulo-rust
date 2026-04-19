/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.macros.MacroExpansion;
import org.rust.lang.core.psi.ProcMacroAttribute;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.ext.RsAttrProcMacroOwner;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsPossibleMacroCallUtil;

import java.util.Collection;
import java.util.function.Consumer;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;

/**
 * Utility methods for working with PSI elements.
 */
public final class PsiUtil {
    private PsiUtil() {
    }

    /**
     * Iterates all children of the PsiElement and invokes the action for each one.
     */
    public static void forEachChild(@NotNull PsiElement element, @NotNull Consumer<PsiElement> action) {
        PsiElement psiChild = element.getFirstChild();
        while (psiChild != null) {
            if (psiChild.getNode() instanceof CompositeElement) {
                action.accept(psiChild);
            }
            psiChild = psiChild.getNextSibling();
        }
    }

    /**
     * Behaves like {@link PsiTreeUtil#findChildrenOfAnyType}, but also collects elements expanded from macros.
     */
    @NotNull
    @SafeVarargs
    public static <T extends PsiElement> Collection<T> findDescendantsWithMacrosOfAnyType(
        @Nullable PsiElement element,
        boolean strict,
        @NotNull Class<? extends T>... classes
    ) {
        if (element == null) return ContainerUtil.emptyList();

        PsiElementProcessor.CollectElements<PsiElement> processor = new PsiElementProcessor.CollectElements<PsiElement>() {
            @Override
            public boolean execute(@NotNull PsiElement each) {
                if (strict && each == element) return true;
                if (PsiTreeUtil.instanceOf(each, classes)) {
                    return super.execute(each);
                }
                return true;
            }
        };
        processElementsWithMacros(element, processor);
        @SuppressWarnings("unchecked")
        Collection<T> result = (Collection<T>) processor.getCollection();
        return result;
    }

    public static boolean processElementsWithMacros(@NotNull PsiElement element,
                                                     @NotNull PsiElementProcessor<PsiElement> processor) {
        return processElementsWithMacros(element, (PsiTreeProcessor) it -> {
            if (processor.execute(it)) {
                return TreeStatus.VISIT_CHILDREN;
            } else {
                return TreeStatus.ABORT;
            }
        });
    }

    /**
     * Behaves like {@link PsiTreeUtil#processElements}, but also collects elements expanded from macros.
     */
    public static boolean processElementsWithMacros(@NotNull PsiElement element,
                                                     @NotNull PsiTreeProcessor processor) {
        if (element instanceof PsiCompiledElement || !element.isPhysical()) {
            // DummyHolders cannot be visited by walking visitors because children/parent relationship is broken there
            TreeStatus status = processor.execute(element);
            switch (status) {
                case VISIT_CHILDREN:
                    break;
                case SKIP_CHILDREN:
                    return true;
                case ABORT:
                    return false;
            }
            for (PsiElement child : element.getChildren()) {
                if (child instanceof RsMacroCall && ((RsMacroCall) child).getMacroArgument() != null) {
                    MacroExpansion expansion = RsPossibleMacroCallUtil.getExpansion((RsMacroCall) child);
                    if (expansion != null) {
                        for (PsiElement expandedElement : expansion.getElements()) {
                            if (!processElementsWithMacros(expandedElement, processor)) return false;
                        }
                    }
                } else if (!processElementsWithMacros(child, processor)) {
                    return false;
                }
            }
            return true;
        }

        RsWithMacrosRecursiveElementWalkingVisitor visitor = new RsWithMacrosRecursiveElementWalkingVisitor(processor);
        visitor.visitElement(element);
        return visitor.getResult();
    }

    private static class RsWithMacrosRecursiveElementWalkingVisitor extends PsiRecursiveElementWalkingVisitor {
        private final PsiTreeProcessor myProcessor;
        private boolean myResult = true;

        RsWithMacrosRecursiveElementWalkingVisitor(@NotNull PsiTreeProcessor processor) {
            this.myProcessor = processor;
        }

        boolean getResult() {
            return myResult;
        }

        @Override
        public void visitElement(@NotNull PsiElement element) {
            /*
             * It is extremely important NOT to call super.visitElement(element.child) here,
             * because it will be equivalent to super.visitElement(element)
             */

            ProcMacroAttribute<RsMetaItem> procMacroAttribute = null;
            if (element instanceof RsAttrProcMacroOwner) {
                procMacroAttribute = ((RsAttrProcMacroOwner) element).getProcMacroAttributeWithDerives();
            }
            if (tryProcessAttrProcMacro(procMacroAttribute)) return;

            TreeStatus status = myProcessor.execute(element);
            switch (status) {
                case VISIT_CHILDREN:
                    if (element instanceof RsMacroCall && shouldExpandMacro((RsMacroCall) element)) {
                        processMacro((RsMacroCall) element, ((RsMacroCall) element).getPath());
                    } else {
                        super.visitElement(element);
                    }
                    tryProcessDeriveProcMacro(procMacroAttribute);
                    break;
                case SKIP_CHILDREN:
                    break;
                case ABORT:
                    stopWalking();
                    myResult = false;
                    break;
            }
        }

        private boolean shouldExpandMacro(@NotNull RsMacroCall element) {
            boolean isWriteMacro = element.getFormatMacroArgument() != null
                && ("write".equals(RsMacroCallUtil.getMacroName(element)) || "writeln".equals(RsMacroCallUtil.getMacroName(element)));
            return element.getMacroArgument() != null || isWriteMacro;
        }

        private void processMacro(@NotNull PsiElement element, @Nullable RsElement path) {
            RsWithMacrosRecursiveElementWalkingVisitor visitor = new RsWithMacrosRecursiveElementWalkingVisitor(myProcessor);
            if (path != null) visitor.visitElement(path);

            if (element instanceof org.rust.lang.core.psi.ext.RsPossibleMacroCall) {
                MacroExpansion expansion = RsPossibleMacroCallUtil.getExpansion((org.rust.lang.core.psi.ext.RsPossibleMacroCall) element);
                if (expansion == null) return;
                for (PsiElement expandedElement : expansion.getElements()) {
                    visitor.visitElement(expandedElement);
                }
            }
        }

        private boolean tryProcessAttrProcMacro(@Nullable ProcMacroAttribute<RsMetaItem> procMacroAttribute) {
            if (!(procMacroAttribute instanceof ProcMacroAttribute.Attr)) return false;
            @SuppressWarnings("unchecked")
            ProcMacroAttribute.Attr<RsMetaItem> attr = (ProcMacroAttribute.Attr<RsMetaItem>) procMacroAttribute;
            processMacro(attr.getAttr(), attr.getAttr().getPath());
            return true;
        }

        private void tryProcessDeriveProcMacro(@Nullable ProcMacroAttribute<RsMetaItem> procMacroAttribute) {
            if (!(procMacroAttribute instanceof ProcMacroAttribute.Derive)) return;
            @SuppressWarnings("unchecked")
            ProcMacroAttribute.Derive<RsMetaItem> derive = (ProcMacroAttribute.Derive<RsMetaItem>) procMacroAttribute;
            for (RsMetaItem deriveItem : derive.getDerives()) {
                processMacro(deriveItem, null);
            }
        }
    }
}
