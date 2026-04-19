/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.consts;

import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.types.KindUtil;
import org.rust.lang.core.types.infer.Node;
import org.rust.lang.core.types.infer.NodeOrValue;
import org.rust.lang.core.types.infer.VarValue;

public class CtInferVar extends Const implements Node {
    @Nullable
    private final Const myOrigin;
    private NodeOrValue myParent;

    public CtInferVar() {
        this(null, new VarValue<>(null, 0));
    }

    public CtInferVar(@Nullable Const origin) {
        this(origin, new VarValue<>(null, 0));
    }

    public CtInferVar(@Nullable Const origin, NodeOrValue parent) {
        super(KindUtil.HAS_CT_INFER_MASK);
        myOrigin = origin;
        myParent = parent;
    }

    @Nullable
    public Const getOrigin() {
        return myOrigin;
    }

    @Override
    public NodeOrValue getParent() {
        return myParent;
    }

    @Override
    public void setParent(NodeOrValue parent) {
        myParent = parent;
    }
}
