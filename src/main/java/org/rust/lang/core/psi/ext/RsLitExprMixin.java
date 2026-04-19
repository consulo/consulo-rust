/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.stubs.IStubElementType;
import org.intellij.lang.regexp.DefaultRegExpPropertiesProvider;
import org.intellij.lang.regexp.RegExpLanguageHost;
import org.intellij.lang.regexp.psi.RegExpChar;
import org.intellij.lang.regexp.psi.RegExpGroup;
import org.intellij.lang.regexp.psi.RegExpNamedGroupRef;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.injected.RsStringLiteralEscaper;
import org.rust.lang.core.psi.RsTokenType;
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.lang.core.psi.impl.RsExprImpl;
import org.rust.lang.core.stubs.RsPlaceholderStub;

public abstract class RsLitExprMixin extends RsExprImpl implements RsLitExpr, RegExpLanguageHost {

    public RsLitExprMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsLitExprMixin(@NotNull RsPlaceholderStub<?> stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @Override
    public boolean isValidHost() {
        return getNode().findChildByType(RsTokenType.RS_ALL_STRING_LITERALS) != null;
    }

    @NotNull
    @Override
    public PsiLanguageInjectionHost updateText(@NotNull String text) {
        ASTNode valueNode = getNode().getFirstChildNode();
        assert valueNode instanceof LeafElement;
        ((LeafElement) valueNode).replaceWithText(text);
        return this;
    }

    @NotNull
    @Override
    public LiteralTextEscaper<RsLitExpr> createLiteralTextEscaper() {
        return RsStringLiteralEscaper.escaperForLiteral(this);
    }

    @NotNull
    @Override
    public PsiReference @NotNull [] getReferences() {
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
    public boolean supportsNamedGroupSyntax(@NotNull RegExpGroup group) {
        return true;
    }

    @Override
    public boolean supportsNamedGroupRefSyntax(@NotNull RegExpNamedGroupRef ref) {
        return ref.isNamedGroupRef();
    }

    @Override
    public boolean supportsExtendedHexCharacter(@NotNull RegExpChar regExpChar) {
        return true;
    }

    @Override
    public boolean isValidCategory(@NotNull String category) {
        return DefaultRegExpPropertiesProvider.getInstance().isValidCategory(category);
    }

    @NotNull
    @Override
    public String[][] getAllKnownProperties() {
        return DefaultRegExpPropertiesProvider.getInstance().getAllKnownProperties();
    }

    @Override
    public String getPropertyDescription(String name) {
        return DefaultRegExpPropertiesProvider.getInstance().getPropertyDescription(name);
    }

    @NotNull
    @Override
    public String[][] getKnownCharacterClasses() {
        return DefaultRegExpPropertiesProvider.getInstance().getKnownCharacterClasses();
    }
}
