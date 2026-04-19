/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractFunction;

public enum ReturnKind {
    VALUE,
    BOOL,
    OPTION_CONTROL_FLOW,
    OPTION_VALUE,
    RESULT,
    TRY_OPERATOR
}
