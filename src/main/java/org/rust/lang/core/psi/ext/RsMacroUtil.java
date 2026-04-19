/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsMacro;
import org.rust.lang.core.psi.RsMacroBody;
import org.rust.lang.core.stubs.RsMacroStub;

public final class RsMacroUtil {
    private RsMacroUtil() {
    }

    @NotNull
    public static final StubbedAttributeProperty<RsMacro, RsMacroStub> HAS_MACRO_EXPORT_PROP =
        new StubbedAttributeProperty<>(QueryAttributes::hasMacroExport, RsMacroStub::getMayHaveMacroExport);

    @NotNull
    public static final StubbedAttributeProperty<RsMacro, RsMacroStub> HAS_MACRO_EXPORT_LOCAL_INNER_MACROS_PROP =
        new StubbedAttributeProperty<>(QueryAttributes::hasMacroExportLocalInnerMacros, RsMacroStub::getMayHaveMacroExportLocalInnerMacros);

    @NotNull
    public static final StubbedAttributeProperty<RsMacro, RsMacroStub> HAS_RUSTC_BUILTIN_MACRO_PROP =
        new StubbedAttributeProperty<>(QueryAttributes::hasRustcBuiltinMacro, RsMacroStub::getMayHaveRustcBuiltinMacro);

    /**
     * "macro_rules" identifier of {@code macro_rules! foo {}}; guaranteed to be non-null by the grammar.
     */
    @NotNull
    public static PsiElement getMacroRules(@NotNull RsMacro macro) {
        return macro.getNode().findChildByType(RsElementTypes.IDENTIFIER).getPsi();
    }

    @Nullable
    public static RsMacroBody getMacroBody(@NotNull RsMacro macro) {
        return RsPsiJavaUtil.childOfType(macro, RsMacroBody.class);
    }

    public static boolean getHasMacroExport(@NotNull RsMacro macro) {
        return HAS_MACRO_EXPORT_PROP.getByPsi(macro);
    }

    public static boolean isRustcDocOnlyMacro(@NotNull RsMacro macro) {
        return isRustcDocOnlyMacro(RsDocAndAttributeOwnerUtil.getQueryAttributes(macro));
    }

    public static boolean isRustcDocOnlyMacro(@NotNull QueryAttributes<?> queryAttributes) {
        return queryAttributes.hasAttribute("rustc_doc_only_macro");
    }
}
