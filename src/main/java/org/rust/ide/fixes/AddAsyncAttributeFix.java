/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.utils.imports.ImportBridge;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsOuterAttr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.KnownItems;

/**
 * <p>
 * Delegates to {@link AddAsyncRecursionAttributeFix} which is the actual class
 */
public class AddAsyncAttributeFix extends RsQuickFixBase<RsFunction> {

    public AddAsyncAttributeFix(@NotNull RsFunction function) {
        super(function);
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.add.async.recursion.attribute");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsFunction element) {
        RsFunction procMacro = KnownItems.knownItems(element)
            .findItem("async_recursion::async_recursion", false, RsFunction.class);
        if (procMacro == null) return;
        ImportBridge.importElement(element, procMacro);
        RsOuterAttr attr = new RsPsiFactory(project).createOuterAttr("async_recursion");
        element.addAfter(attr, null);
    }

    @Nullable
    public static AddAsyncAttributeFix createIfCompatible(@NotNull RsFunction function) {
        if (!hasAsyncRecursionDependency(function)) return null;
        return new AddAsyncAttributeFix(function);
    }

    private static boolean hasAsyncRecursionDependency(@NotNull RsElement context) {
        var crate = RsElementUtil.getContainingCrate(context);
        return crate.getDependencies().stream().anyMatch(dep -> "async_recursion".equals(dep.getNormName()));
    }
}
