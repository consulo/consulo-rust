/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IStubElementType;
import consulo.language.ast.TokenSet;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.resolve.ref.*;
import org.rust.lang.core.stubs.RsPathStub;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.psi.ext.*;

public abstract class RsPathImplMixin extends RsStubbedElementImpl<RsPathStub> implements RsPath {

    private static final TokenSet RS_PATH_KINDS = RsTokenType.tokenSetOf(
        RsElementTypes.IDENTIFIER, RsElementTypes.SELF, RsElementTypes.SUPER,
        RsElementTypes.CSELF, RsElementTypes.CRATE
    );

    public RsPathImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsPathImplMixin(@Nonnull RsPathStub stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Nullable
    @Override
    public RsPathReference getReference() {
        if (getReferenceName() == null) return null;
        PsiElement parent = getParent();
        if (parent instanceof RsMacroCall) {
            return new RsMacroPathReferenceImpl(this);
        }
        if (parent instanceof RsMetaItem) {
            RsMetaItem meta = (RsMetaItem) parent;
            if (RsPsiPattern.derivedTraitMetaItem.accepts(meta)) {
                return new RsDeriveTraitReferenceImpl(this);
            }
            if (RsPsiPattern.nonStdOuterAttributeMetaItem.accepts(meta)) {
                return new RsAttributeProcMacroReferenceImpl(this);
            }
            return null;
        }
        if (parent instanceof RsPath) {
            PsiElement rootPathParent = RsPathUtil.rootPath((RsPath) parent).getParent();
            if (!(rootPathParent instanceof RsMetaItem) || RsPsiPattern.nonStdOuterAttributeMetaItem.accepts(rootPathParent)) {
                return new RsPathReferenceImpl(this);
            }
            return null;
        }
        return new RsPathReferenceImpl(this);
    }

    @Nullable
    @Override
    public PsiElement getReferenceNameElement() {
        ASTNode child = getNode().findChildByType(RS_PATH_KINDS);
        return child != null ? child.getPsi() : null;
    }

    @Nullable
    @Override
    public String getReferenceName() {
        RsPathStub stub = getGreenStub() instanceof RsPathStub ? (RsPathStub) getGreenStub() : null;
        if (stub != null) {
            return stub.getReferenceName();
        }
        PsiElement refNameElement = getReferenceNameElement();
        return refNameElement != null ? RsPsiUtilUtil.getUnescapedText(refNameElement) : null;
    }

    @Nonnull
    @Override
    public RsMod getContainingMod() {
        RsPath rootPath = RsPathUtil.rootPath(this);
        PsiElement visParent = rootPath.getParent();
        if (visParent instanceof RsVisRestriction) {
            PsiElement visGrandParent = visParent.getParent();
            if (visGrandParent != null) {
                PsiElement visGGParent = visGrandParent.getParent();
                if (visGGParent instanceof RsMod) {
                    return ((RsMod) visGGParent).getContainingMod();
                }
            }
        }
        return super.getContainingMod();
    }

    @Override
    public boolean getHasColonColon() {
        Object stub = getGreenStub();
        if (stub instanceof RsPathStub) {
            return ((RsPathStub) stub).getHasColonColon();
        }
        return getColoncolon() != null;
    }

    @Nonnull
    @Override
    public PathKind getKind() {
        Object stub = getGreenStub();
        if (stub instanceof RsPathStub) {
            return ((RsPathStub) stub).getKind();
        }
        ASTNode child = getNode().findChildByType(RS_PATH_KINDS);
        if (child == null) return PathKind.MALFORMED;
        if (child.getElementType() == RsElementTypes.IDENTIFIER) return PathKind.IDENTIFIER;
        if (child.getElementType() == RsElementTypes.SELF) return PathKind.SELF;
        if (child.getElementType() == RsElementTypes.SUPER) return PathKind.SUPER;
        if (child.getElementType() == RsElementTypes.CSELF) return PathKind.CSELF;
        if (child.getElementType() == RsElementTypes.CRATE) return PathKind.CRATE;
        return PathKind.MALFORMED;
    }
}
