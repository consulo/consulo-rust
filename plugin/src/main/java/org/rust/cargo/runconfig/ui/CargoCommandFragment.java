/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.ui;

import consulo.language.editor.ui.awt.TextFieldCompletionProvider;
import org.jspecify.annotations.Nullable;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.ide.cargo.completion.CargoCommandCompletionProvider;

import java.util.function.Supplier;

public class CargoCommandFragment extends RsCommandFragment<CargoCommandConfiguration> {
    @Override
    protected TextFieldCompletionProvider createCompletionProvider(CargoProjectsService projects,
                                                                   Supplier<@Nullable CargoWorkspace> workspace) {
        return new CargoCommandCompletionProvider(projects, workspace);
    }
}
