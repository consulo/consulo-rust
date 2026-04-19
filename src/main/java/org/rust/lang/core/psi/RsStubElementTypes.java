/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IElementType;
import org.rust.lang.core.psi.ext.RsElement;

/** Contains constants from {@link RsElementTypes} with correct types */
public final class RsStubElementTypes {

    @SuppressWarnings("unchecked")
    public static final IStubElementType<StubElement<RsIncludeMacroArgument>, RsIncludeMacroArgument> INCLUDE_MACRO_ARGUMENT =
        (IStubElementType<StubElement<RsIncludeMacroArgument>, RsIncludeMacroArgument>) (IElementType) RsElementTypes.INCLUDE_MACRO_ARGUMENT;

    @SuppressWarnings("unchecked")
    public static final IStubElementType<StubElement<RsUseGroup>, RsUseGroup> USE_GROUP =
        (IStubElementType<StubElement<RsUseGroup>, RsUseGroup>) (IElementType) RsElementTypes.USE_GROUP;

    @SuppressWarnings("unchecked")
    public static final IStubElementType<StubElement<RsEnumBody>, RsEnumBody> ENUM_BODY =
        (IStubElementType<StubElement<RsEnumBody>, RsEnumBody>) (IElementType) RsElementTypes.ENUM_BODY;

    @SuppressWarnings("unchecked")
    public static final IStubElementType<StubElement<RsBlockFields>, RsBlockFields> BLOCK_FIELDS =
        (IStubElementType<StubElement<RsBlockFields>, RsBlockFields>) (IElementType) RsElementTypes.BLOCK_FIELDS;

    @SuppressWarnings("unchecked")
    public static final IStubElementType<StubElement<RsVisRestriction>, RsVisRestriction> VIS_RESTRICTION =
        (IStubElementType<StubElement<RsVisRestriction>, RsVisRestriction>) (IElementType) RsElementTypes.VIS_RESTRICTION;

    private RsStubElementTypes() {}
}
