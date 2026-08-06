/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.introduceConstant;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsExpr;

import java.util.List;

public interface ExtractConstantUi {
    @Nonnull
    InsertionCandidate chooseInsertionPoint(@Nonnull RsExpr expr, @Nonnull List<InsertionCandidate> candidates);
}
