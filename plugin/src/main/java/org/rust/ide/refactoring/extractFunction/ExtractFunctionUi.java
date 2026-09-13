/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractFunction;

import consulo.project.Project;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.language.editor.refactoring.ui.MethodSignatureComponent;
import consulo.language.editor.refactoring.ui.NameSuggestionsField;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.rust.RsBundle;
import org.rust.lang.core.names.RsNamesValidator;
import org.rust.lang.RsFileType;
import org.rust.openapiext.OpenApiUtil;

public interface ExtractFunctionUi {
    void extract(@Nonnull RsExtractFunctionConfig config, @Nonnull Runnable callback);

    @Nullable
    ExtractFunctionUi MOCK_HOLDER = null;

    static void extractFunctionDialog(
        @Nonnull Project project,
        @Nonnull RsExtractFunctionConfig config,
        @Nonnull Runnable callback
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

    
    static void withMockExtractFunctionUi(@Nonnull ExtractFunctionUi mockUi, @Nonnull Runnable action) {
        ExtractFunctionUiHolder.MOCK = mockUi;
        try {
            action.run();
        } finally {
            ExtractFunctionUiHolder.MOCK = null;
        }
    }
}
