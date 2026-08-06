/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move.common;

import consulo.language.psi.PsiReference;
import consulo.usage.UsageInfo;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsElement;

public abstract class RsMoveUsageInfo extends UsageInfo {

    @Nonnull
    private final RsElement rsElement;

    protected RsMoveUsageInfo(@Nonnull RsElement element) {
        super(element);
        this.rsElement = element;
    }

    @Nonnull
    public RsElement getRsElement() {
        return rsElement;
    }
}
