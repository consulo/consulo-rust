/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.introduceConstant;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsExpr;

import java.util.List;

public interface ExtractConstantUi {
    @NotNull
    InsertionCandidate chooseInsertionPoint(@NotNull RsExpr expr, @NotNull List<InsertionCandidate> candidates);
}
