/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.icons.RsIcons;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsItemElement;
import org.rust.lang.core.psi.ext.RsItemsOwnerUtil;
import org.rust.openapiext.EditorExt;

import java.util.List;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class RsFnMainCompletionProvider extends RsCompletionProvider {
    public static final RsFnMainCompletionProvider INSTANCE = new RsFnMainCompletionProvider();

    private RsFnMainCompletionProvider() {
    }

    @NotNull
    @Override
    public ElementPattern<PsiElement> getElementPattern() {
        return PlatformPatterns.psiElement()
            .with(new com.intellij.patterns.PatternCondition<PsiElement>("Is incomplete function declaration") {
                @Override
                public boolean accepts(@NotNull PsiElement it, ProcessingContext ctx) {
                    PsiElement element = Utils.getOriginalOrSelf(it);
                    PsiElement parent = element.getParent();
                    if (parent instanceof RsFile) {
                        PsiElement prev = prevLeafSkippingWhitespace(element);
                        return !(prev instanceof PsiErrorElement);
                    } else if (parent instanceof RsFunction) {
                        RsFunction fn = (RsFunction) parent;
                        return RsFunctionUtil.getBlock(fn) == null
                            && fn.getTypeParameterList() == null
                            && fn.getValueParameterList() == null
                            && fn.getContext() instanceof RsFile;
                    }
                    return false;
                }
            });
    }

    private static PsiElement prevLeafSkippingWhitespace(PsiElement element) {
        PsiElement prev = element.getPrevSibling();
        while (prev instanceof PsiWhiteSpace) {
            prev = prev.getPrevSibling();
        }
        return prev;
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        PsiElement element = parameters.getPosition();
        if (!shouldAddCompletion(element)) return;

        boolean hasFnKeyword = false;
        PsiElement prevSibling = element.getPrevSibling();
        while (prevSibling instanceof PsiWhiteSpace) {
            prevSibling = prevSibling.getPrevSibling();
        }
        if (prevSibling != null && prevSibling.getNode().getElementType() == RsElementTypes.FN) {
            hasFnKeyword = true;
        }

        String fnPrefix = hasFnKeyword ? "" : "fn ";
        LookupElementBuilder lookup = LookupElementBuilder
            .create(fnPrefix + "main() {\n    \n}")
            .withPresentableText(fnPrefix + "main() { ... }")
            .withIcon(RsIcons.FUNCTION)
            .withInsertHandler((insertionContext, item) -> {
                RsFunction function = LookupElements.getElementOfType(insertionContext, RsFunction.class);
                if (function != null) {
                    EditorExt.moveCaretToOffset(insertionContext.getEditor(), function, insertionContext.getTailOffset() - "\n}".length());
                }
            });
        result.addElement(
            LookupElements.toRsLookupElement(lookup, new RsLookupElementProperties(true))
        );
    }

    private static boolean shouldAddCompletion(PsiElement element) {
        RsFile file = RsElementUtil.contextualFile(element) instanceof RsFile ? (RsFile) RsElementUtil.contextualFile(element) : null;
        if (file == null) return false;
        file = (RsFile) file.getOriginalFile();
        org.rust.lang.core.crate.Crate crate = file.getCrate();
        if (crate == null) return false;
        RsFile rootMod = crate.getRootMod();
        if (file != rootMod) return false;
        if (!crate.getKind().canHaveMainFunction()) return false;
        List<RsItemElement> mainItems = RsItemsOwnerUtil.getExpandedItemsCached(file).getNamed().get("main");
        if (mainItems == null) return true;
        for (RsItemElement item : mainItems) {
            if (item instanceof RsFunction) return false;
        }
        return true;
    }
}
