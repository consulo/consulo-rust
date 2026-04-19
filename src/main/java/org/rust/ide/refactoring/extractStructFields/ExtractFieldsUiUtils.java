/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractStructFields;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.rust.openapiext.OpenApiUtil;

public final class ExtractFieldsUiUtils {
    @Nullable
    private static ExtractFieldsUi MOCK = null;

    private ExtractFieldsUiUtils() {
    }

    @Nullable
    public static String showExtractStructFieldsDialog(@NotNull Project project) {
        ExtractFieldsUi chooser;
        if (org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
            chooser = MOCK;
            if (chooser == null) {
                throw new IllegalStateException("You should set mock ui via `withMockExtractFieldsUi`");
            }
        } else {
            chooser = new ExtractFieldsDialog(project);
        }
        return chooser.selectStructName(project);
    }

    @TestOnly
    public static void withMockExtractFieldsUi(@NotNull ExtractFieldsUi mockUi, @NotNull Runnable action) {
        MOCK = mockUi;
        try {
            action.run();
        } finally {
            MOCK = null;
        }
    }
}
