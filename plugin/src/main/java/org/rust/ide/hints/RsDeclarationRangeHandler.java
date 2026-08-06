/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints;


public class RsDeclarationRangeHandler {

    private RsDeclarationRangeHandler() {
    }

    /** @deprecated Use {@link RsStructItemDeclarationRangeHandler} directly. */
    @Deprecated
    public static class StructItem extends RsStructItemDeclarationRangeHandler {
    }

    /** @deprecated Use {@link RsTraitItemDeclarationRangeHandler} directly. */
    @Deprecated
    public static class TraitItem extends RsTraitItemDeclarationRangeHandler {
    }

    /** @deprecated Use {@link RsImplItemDeclarationRangeHandler} directly. */
    @Deprecated
    public static class ImplItem extends RsImplItemDeclarationRangeHandler {
    }

    /** @deprecated Use {@link RsEnumItemDeclarationRangeHandler} directly. */
    @Deprecated
    public static class EnumItem extends RsEnumItemDeclarationRangeHandler {
    }

    /** @deprecated Use {@link RsModItemDeclarationRangeHandler} directly. */
    @Deprecated
    public static class ModItem extends RsModItemDeclarationRangeHandler {
    }

    /** @deprecated Use {@link RsFunctionDeclarationRangeHandler} directly. */
    @Deprecated
    public static class Function extends RsFunctionDeclarationRangeHandler {
    }

    /** @deprecated Use {@link RsMacroDeclarationRangeHandler} directly. */
    @Deprecated
    public static class Macro extends RsMacroDeclarationRangeHandler {
    }
}
