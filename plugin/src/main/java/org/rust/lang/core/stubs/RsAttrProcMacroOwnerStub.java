/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import consulo.util.lang.BitUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwner;
import org.rust.lang.core.stubs.common.RsAttrProcMacroOwnerPsiOrStub;
import org.rust.stdext.HashCode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.PsiWhiteSpace;
import org.rust.lang.core.psi.RsTokenSets;
import org.rust.lang.core.psi.ext.RsAttr;

public interface RsAttrProcMacroOwnerStub extends RsAttributeOwnerStub, RsAttrProcMacroOwnerPsiOrStub<RsMetaItemStub> {
    @Nullable
    String getStubbedText();

    @Nullable
    HashCode getStubbedTextHash();

    int getEndOfAttrsOffset();

    int getStartOffset();

    /**
     * Extract stubbed text + body hash + offsets for proc-macro stub creation. Returns
     * {@code null} unless the owner may have custom-derive or custom-attribute proc macros.
     */
    @Nullable
    static RsProcMacroStubInfo extractTextAndOffset(int flags, @Nonnull RsDocAndAttributeOwner psi) {
        boolean isProcMacro =
            BitUtil.isSet(flags, RsAttributeOwnerStub.CommonStubAttrFlags.MAY_HAVE_CUSTOM_DERIVE)
                || BitUtil.isSet(flags, RsAttributeOwnerStub.CommonStubAttrFlags.MAY_HAVE_CUSTOM_ATTRS);
        if (!isProcMacro) return null;
        String stubbedText = psi.getText();
        if (stubbedText == null) return null;
        HashCode hash = !BitUtil.isSet(flags, RsAttributeOwnerStub.CommonStubAttrFlags.HAS_CFG_ATTR)
            ? HashCode.compute(stubbedText)
            : null;
        int startOffset = psi.getTextRange() != null ? psi.getTextRange().getStartOffset() : 0;
        return new RsProcMacroStubInfo(stubbedText, hash, endOfAttrsOffset(psi), startOffset);
    }

    /**
     * Offset inside {@code psi} of the first child that is not an attribute, a comment or whitespace,
     * i.e. where the item itself starts. {@code 0} when there is no such child.
     */
    static int endOfAttrsOffset(@Nonnull RsDocAndAttributeOwner psi) {
        for (PsiElement child = psi.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof RsAttr) continue;
            if (child instanceof PsiWhiteSpace) continue;
            if (RsTokenSets.RS_COMMENTS.contains(
                PsiUtilCore.getElementType(child))) {
                continue;
            }
            return child.getStartOffsetInParent();
        }
        return 0;
    }
}
