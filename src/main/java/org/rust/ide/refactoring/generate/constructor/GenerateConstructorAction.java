/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate.constructor;

import org.jetbrains.annotations.NotNull;
import org.rust.ide.refactoring.generate.BaseGenerateAction;
import org.rust.ide.refactoring.generate.BaseGenerateHandler;

public class GenerateConstructorAction extends BaseGenerateAction {
    @NotNull
    @Override
    protected BaseGenerateHandler getGenerateHandler() {
        return new GenerateConstructorHandler();
    }
}
