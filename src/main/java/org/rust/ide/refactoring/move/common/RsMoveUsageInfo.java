/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move.common;

import com.intellij.psi.PsiReference;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsElement;

public abstract class RsMoveUsageInfo extends UsageInfo {

    @NotNull
    private final RsElement rsElement;

    protected RsMoveUsageInfo(@NotNull RsElement element) {
        super(element);
        this.rsElement = element;
    }

    @NotNull
    public RsElement getRsElement() {
        return rsElement;
    }
}
