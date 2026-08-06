/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

public abstract class LogicOp extends BoolOp {
    public static final LogicOp AND = new LogicOp() {};
    public static final LogicOp OR = new LogicOp() {};
}
