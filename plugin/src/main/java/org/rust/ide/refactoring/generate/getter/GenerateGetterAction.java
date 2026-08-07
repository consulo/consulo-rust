/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate.getter;

import jakarta.annotation.Nonnull;
import org.rust.ide.refactoring.generate.BaseGenerateAction;
import org.rust.ide.refactoring.generate.BaseGenerateHandler;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.annotation.component.ActionRef;

@ActionImpl(
    id = "Rust.GenerateGetter",
    parents = @ActionParentRef(
        value = @ActionRef(id = "GenerateGroup")
    )
)
public class GenerateGetterAction extends BaseGenerateAction {
    @Nonnull
    @Override
    protected BaseGenerateHandler getGenerateHandler() {
        return new GenerateGetterHandler();
    }
}
