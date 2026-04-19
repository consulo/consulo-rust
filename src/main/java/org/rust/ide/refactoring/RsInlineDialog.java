/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import com.intellij.openapi.project.Project;
import com.intellij.refactoring.inline.InlineOptionsDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsNameIdentifierOwner;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.openapiext.OpenApiUtil;

public abstract class RsInlineDialog extends InlineOptionsDialog {
    @Nullable
    private final RsReference myRefElement;

    protected RsInlineDialog(
        @NotNull RsNameIdentifierOwner element,
        @Nullable RsReference refElement,
        @NotNull Project project
    ) {
        super(project, true, element);
        myRefElement = refElement;
    }

    @NotNull
    protected String getOccurrencesText(int occurrences) {
        if (occurrences < 0) {
            return "";
        } else if (occurrences == 1) {
            return "has 1 occurrence";
        } else {
            return "has " + occurrences + " occurrences";
        }
    }

    @Override
    public boolean isInlineThis() {
        return false;
    }

    @Override
    protected final void init() {
        if (org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
            throw new IllegalStateException("Check failed.");
        }
        setTitle(getBorderTitle());
        myInvokedOnReference = myRefElement != null;
        setPreviewResults(true);
        super.init();
    }
}
