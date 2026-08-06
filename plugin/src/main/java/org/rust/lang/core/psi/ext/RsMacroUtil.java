/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsMacro;
import org.rust.lang.core.psi.RsMacroBody;
import org.rust.lang.core.stubs.RsMacroStub;

public final class RsMacroUtil {
    private RsMacroUtil() {
    }

    @Nonnull
    public static final StubbedAttributeProperty<RsMacro, RsMacroStub> HAS_MACRO_EXPORT_PROP =
        new StubbedAttributeProperty<>(QueryAttributes::hasMacroExport, RsMacroStub::getMayHaveMacroExport);

    @Nonnull
    public static final StubbedAttributeProperty<RsMacro, RsMacroStub> HAS_MACRO_EXPORT_LOCAL_INNER_MACROS_PROP =
        new StubbedAttributeProperty<>(QueryAttributes::hasMacroExportLocalInnerMacros, RsMacroStub::getMayHaveMacroExportLocalInnerMacros);

    @Nonnull
    public static final StubbedAttributeProperty<RsMacro, RsMacroStub> HAS_RUSTC_BUILTIN_MACRO_PROP =
        new StubbedAttributeProperty<>(QueryAttributes::hasRustcBuiltinMacro, RsMacroStub::getMayHaveRustcBuiltinMacro);

    /**
     * "macro_rules" identifier of {@code macro_rules! foo {}}; guaranteed to be non-null by the grammar.
     */
    @Nonnull
    public static PsiElement getMacroRules(@Nonnull RsMacro macro) {
        return macro.getNode().findChildByType(RsElementTypes.IDENTIFIER).getPsi();
    }

    @Nullable
    public static RsMacroBody getMacroBody(@Nonnull RsMacro macro) {
        return RsPsiJavaUtil.childOfType(macro, RsMacroBody.class);
    }

    public static boolean getHasMacroExport(@Nonnull RsMacro macro) {
        return HAS_MACRO_EXPORT_PROP.getByPsi(macro);
    }

    public static boolean isRustcDocOnlyMacro(@Nonnull RsMacro macro) {
        return isRustcDocOnlyMacro(RsDocAndAttributeOwnerUtil.getQueryAttributes(macro));
    }

    public static boolean isRustcDocOnlyMacro(@Nonnull QueryAttributes<?> queryAttributes) {
        return queryAttributes.hasAttribute("rustc_doc_only_macro");
    }
}
