/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

    public AddAsyncAttributeFix(@Nonnull RsFunction function) {
        super(function);
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.add.async.recursion.attribute"));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsFunction element) {
        RsFunction procMacro = KnownItems.knownItems(element)
            .findItem("async_recursion::async_recursion", false, RsFunction.class);
        if (procMacro == null) return;
        ImportBridge.importElement(element, procMacro);
        RsOuterAttr attr = new RsPsiFactory(project).createOuterAttr("async_recursion");
        element.addAfter(attr, null);
    }

    @Nullable
    public static AddAsyncAttributeFix createIfCompatible(@Nonnull RsFunction function) {
        if (!hasAsyncRecursionDependency(function)) return null;
        return new AddAsyncAttributeFix(function);
    }

    private static boolean hasAsyncRecursionDependency(@Nonnull RsElement context) {
        var crate = RsElementUtil.getContainingCrate(context);
        return crate.getDependencies().stream().anyMatch(dep -> "async_recursion".equals(dep.getNormName()));
    }
}
