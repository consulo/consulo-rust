/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiReference;
import consulo.language.psi.ReferenceProvidersRegistry;
import consulo.language.impl.psi.LazyParseablePsiElement;
import consulo.language.ast.IElementType;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwner;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.impl.RsPsiJavaUtil;
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

    public RsDocCommentImpl(@Nonnull IElementType type, @Nullable CharSequence text) {
        super(type, text);
    }

    @Nonnull
    @Override
    public RsMod getContainingMod() {
        RsMod mod = PsiTreeUtil.getContextOfType(
            CompletionUtilCore.getOriginalOrSelf(this), RsMod.class, true
        );
        if (mod != null) {
            return CompletionUtilCore.getOriginalOrSelf(mod);
        }
        throw new IllegalStateException("Element outside of module: " + getText());
    }

    @Override
    @Nonnull
    public IElementType getTokenType() {
        return getNode().getElementType();
    }

    /** Needed for URL references ({@link consulo.language.impl.psi.path.WebReference}) */
    @Override
    @Nonnull
    public PsiReference[] getReferences() {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this);
    }

    // Needed for RsFoldingBuilder
    @Override
    public void accept(@Nonnull PsiElementVisitor visitor) {
        visitor.visitComment(this);
    }

    @Override
    @Nonnull
    public String toString() {
        return "PsiComment(" + getNode().getElementType() + ")";
    }

    @Override
    @Nullable
    public RsDocAndAttributeOwner getOwner() {
        return RsPsiJavaUtil.ancestorStrict(this, RsDocAndAttributeOwner.class);
    }

    @Override
    @Nonnull
    public List<RsDocCodeFence> getCodeFences() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, RsDocCodeFence.class);
    }

    @Override
    @Nonnull
    public List<RsDocLinkDefinition> getLinkDefinitions() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, RsDocLinkDefinition.class);
    }

    @Override
    @Nonnull
    public Map<String, RsDocLinkDefinition> getLinkReferenceMap() {
        return CachedValuesManager.getManager(this.getProject()).getCachedValue(this, () -> {
            Map<String, RsDocLinkDefinition> result = new HashMap<>();
            for (RsDocLinkDefinition def : getLinkDefinitions()) {
                result.put(def.getLinkLabel().getMarkdownValue(), def);
            }
            return CachedValueProvider.Result.create(result, getContainingFile());
        });
    }

    @Nullable
    @Override
    public org.rust.lang.core.psi.ext.RsMod getCrateRoot() {
        return org.rust.lang.core.psi.ext.impl.RsElementUtil.getCrateRoot(this);
    }

    @Nonnull
    @Override
    public org.rust.lang.core.crate.Crate getContainingCrate() {
        return org.rust.lang.core.psi.ext.impl.RsElementUtil.getContainingCrate(this);
    }
}
