/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwner;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.doc.psi.RsDocCodeFence;
import org.rust.lang.doc.psi.RsDocComment;
import org.rust.lang.doc.psi.RsDocLinkDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @param type the element type
 * @param text a text for lazy parsing. {@code null} value means that the element is parsed ({@code isParsed()} is {@code true})
 */
public class RsDocCommentImpl extends LazyParseablePsiElement implements RsDocComment {

    public RsDocCommentImpl(@NotNull IElementType type, @Nullable CharSequence text) {
        super(type, text);
    }

    @NotNull
    @Override
    public RsMod getContainingMod() {
        RsMod mod = PsiTreeUtil.getContextOfType(
            CompletionUtil.getOriginalOrSelf(this), RsMod.class, true
        );
        if (mod != null) {
            return CompletionUtil.getOriginalOrSelf(mod);
        }
        throw new IllegalStateException("Element outside of module: " + getText());
    }

    @Override
    @NotNull
    public IElementType getTokenType() {
        return getNode().getElementType();
    }

    /** Needed for URL references ({@link com.intellij.openapi.paths.WebReference}) */
    @Override
    @NotNull
    public PsiReference[] getReferences() {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this);
    }

    // Needed for RsFoldingBuilder
    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        visitor.visitComment(this);
    }

    @Override
    @NotNull
    public String toString() {
        return "PsiComment(" + getNode().getElementType() + ")";
    }

    @Override
    @Nullable
    public RsDocAndAttributeOwner getOwner() {
        return RsPsiJavaUtil.ancestorStrict(this, RsDocAndAttributeOwner.class);
    }

    @Override
    @NotNull
    public List<RsDocCodeFence> getCodeFences() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, RsDocCodeFence.class);
    }

    @Override
    @NotNull
    public List<RsDocLinkDefinition> getLinkDefinitions() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, RsDocLinkDefinition.class);
    }

    @Override
    @NotNull
    public Map<String, RsDocLinkDefinition> getLinkReferenceMap() {
        return CachedValuesManager.getCachedValue(this, () -> {
            Map<String, RsDocLinkDefinition> result = new HashMap<>();
            for (RsDocLinkDefinition def : getLinkDefinitions()) {
                result.put(def.getLinkLabel().getMarkdownValue(), def);
            }
            return CachedValueProvider.Result.create(result, getContainingFile());
        });
    }
}
