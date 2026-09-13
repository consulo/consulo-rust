/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.RsExpr;

/**
 *
 * An expression which can contain a {@link RsLabelReferenceOwner}.
 */
public interface RsLooplikeExpr extends RsExpr, RsLabeledExpression, RsOuterAttributeOwner {
}
