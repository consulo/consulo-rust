/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractFunction;

import org.jetbrains.annotations.Nullable;

final class ExtractFunctionUiHolder {
    @Nullable
    static ExtractFunctionUi MOCK = null;

    private ExtractFunctionUiHolder() {
    }
}
