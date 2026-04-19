/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwner;
import org.rust.lang.core.stubs.common.RsAttrProcMacroOwnerPsiOrStub;
import org.rust.stdext.HashCode;

public interface RsAttrProcMacroOwnerStub extends RsAttributeOwnerStub, RsAttrProcMacroOwnerPsiOrStub<RsMetaItemStub> {
    @Nullable
    String getStubbedText();

    @Nullable
    HashCode getStubbedTextHash();

    int getEndOfAttrsOffset();

    int getStartOffset();

    /**
     * Extract stubbed text + body hash + offsets for proc-macro stub creation. Mirrors
     * {@code fun RsAttrProcMacroOwnerStub.Companion.extractTextAndOffset(flags, psi)} from
     * no custom-derive or custom-attribute proc macros.
     * <p>
     * The {@code endOfAttrsOffset} stays {@code 0} until {@code RsAttrProcMacroOwner.endOfAttrsOffset}
     * is ported on the Java side; consumers fall back to the full text range when this is zero.
     */
    @Nullable
    static RsProcMacroStubInfo extractTextAndOffset(int flags, @NotNull RsDocAndAttributeOwner psi) {
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
        return new RsProcMacroStubInfo(stubbedText, hash, 0, startOffset);
    }
}
