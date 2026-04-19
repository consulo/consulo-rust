/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa.borrowck;

public enum MoveKind {
    Declared, MovePat, Captured, MoveExpr
}
