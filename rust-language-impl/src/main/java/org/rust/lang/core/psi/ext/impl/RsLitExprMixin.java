/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceService;
import consulo.language.psi.LiteralTextEscaper;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.impl.ast.LeafElement;
import consulo.language.psi.stub.IStubElementType;
import org.intellij.lang.regexp.DefaultRegExpPropertiesProvider;
import org.intellij.lang.regexp.RegExpLanguageHost;
import org.intellij.lang.regexp.psi.RegExpChar;
import org.intellij.lang.regexp.psi.RegExpGroup;
import org.intellij.lang.regexp.psi.RegExpNamedGroupRef;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.injected.RsStringLiteralEscaper;
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.lang.core.psi.impl.RsExprImpl;
import org.rust.lang.core.stubs.RsPlaceholderStub;
import org.rust.lang.core.psi.impl.RsTokenSets;
import org.rust.lang.core.psi.ext.*;

public abstract class RsLitExprMixin extends RsExprImpl implements RsLitExpr, RegExpLanguageHost {

    public RsLitExprMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsLitExprMixin(@Nonnull RsPlaceholderStub<?> stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    public Class<? extends PsiLanguageInjectionHost> getHostClass() {
        return RsLitExprMixin.class;
    }

    @Override
    public boolean isValidHost() {
        return getNode().findChildByType(RsTokenSets.RS_ALL_STRING_LITERALS) != null;
    }

    @Nonnull
    @Override
    public PsiLanguageInjectionHost updateText(@Nonnull String text) {
        ASTNode valueNode = getNode().getFirstChildNode();
        assert valueNode instanceof LeafElement;
        ((LeafElement) valueNode).replaceWithText(text);
        return this;
    }

    @Nonnull
    @Override
    public LiteralTextEscaper<RsLitExpr> createLiteralTextEscaper() {
        return RsStringLiteralEscaper.escaperForLiteral(this);
    }

    @Nonnull
    @Override
    public PsiReference [] getReferences() {
        return PsiReferenceService.getService().getContributedReferences(this);
    }

    @Override
    public boolean characterNeedsEscaping(char c) {
        return false;
    }

    @Override
    public boolean supportsPerl5EmbeddedComments() {
        return false;
    }

    @Override
    public boolean supportsPossessiveQuantifiers() {
        return true;
    }

    @Override
    public boolean supportsPythonConditionalRefs() {
        return false;
    }

    @Override
    public boolean supportsNamedGroupSyntax(@Nonnull RegExpGroup group) {
        return true;
    }

    @Override
    public boolean supportsNamedGroupRefSyntax(@Nonnull RegExpNamedGroupRef ref) {
        return ref.isNamedGroupRef();
    }

    @Override
    public boolean supportsExtendedHexCharacter(@Nonnull RegExpChar regExpChar) {
        return true;
    }

    @Override
    public boolean isValidCategory(@Nonnull String category) {
        return DefaultRegExpPropertiesProvider.getInstance().isValidCategory(category);
    }

    @Nonnull
    @Override
    public String[][] getAllKnownProperties() {
        return DefaultRegExpPropertiesProvider.getInstance().getAllKnownProperties();
    }

    @Override
    public String getPropertyDescription(String name) {
        return DefaultRegExpPropertiesProvider.getInstance().getPropertyDescription(name);
    }

    @Nonnull
    @Override
    public String[][] getKnownCharacterClasses() {
        return DefaultRegExpPropertiesProvider.getInstance().getKnownCharacterClasses();
    }
}
