/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.MacroBraces;
import org.rust.lang.core.psi.RsMacroBody;
import org.rust.stdext.HashCode;

/**
 *
 * Represents {@link org.rust.lang.core.psi.RsMacro} or {@link org.rust.lang.core.psi.RsMacro2}.
 */
public interface RsMacroDefinitionBase extends RsNameIdentifierOwner,
        RsQualifiedNamedElement,
        RsOuterAttributeOwner,
        RsExpandedElement,
        RsAttrProcMacroOwner,
        RsModificationTrackerOwner {

    @Nullable
    RsMacroBody getMacroBodyStubbed();

    @Nullable
    HashCode getBodyHash();

    boolean getHasRustcBuiltinMacro();

    @NotNull
    MacroBraces getPreferredBraces();
}
