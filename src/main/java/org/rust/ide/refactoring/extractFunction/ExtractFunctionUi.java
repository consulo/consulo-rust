/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractFunction;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.refactoring.ui.MethodSignatureComponent;
import com.intellij.refactoring.ui.NameSuggestionsField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.rust.RsBundle;
import org.rust.ide.refactoring.RsNamesValidator;
import org.rust.lang.RsFileType;
import org.rust.openapiext.OpenApiUtil;

public interface ExtractFunctionUi {
    void extract(@NotNull RsExtractFunctionConfig config, @NotNull Runnable callback);

    @Nullable
    ExtractFunctionUi MOCK_HOLDER = null;

    static void extractFunctionDialog(
        @NotNull Project project,
        @NotNull RsExtractFunctionConfig config,
        @NotNull Runnable callback
    ) {
        ExtractFunctionUi ui;
        if (org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
            ui = ExtractFunctionUiHolder.MOCK;
            if (ui == null) {
                throw new IllegalStateException("You should set mock ui via `withMockExtractFunctionUi`");
            }
        } else {
            ui = new DialogExtractFunctionUi(project);
        }
        ui.extract(config, callback);
    }

    @TestOnly
    static void withMockExtractFunctionUi(@NotNull ExtractFunctionUi mockUi, @NotNull Runnable action) {
        ExtractFunctionUiHolder.MOCK = mockUi;
        try {
            action.run();
        } finally {
            ExtractFunctionUiHolder.MOCK = null;
        }
    }
}
