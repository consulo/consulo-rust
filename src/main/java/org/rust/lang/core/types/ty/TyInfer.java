/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.types.KindUtil;
import org.rust.lang.core.types.infer.Node;
import org.rust.lang.core.types.infer.NodeOrValue;
import org.rust.lang.core.types.infer.VarValue;

public abstract class TyInfer extends Ty {
    protected TyInfer(int flags) {
        super(flags);
    }

    public static class TyVar extends TyInfer implements Node {
        @Nullable
        private final Ty myOrigin;
        private NodeOrValue myParent;

        public TyVar() {
            this(null);
        }

        public TyVar(@Nullable Ty origin) {
            super(KindUtil.HAS_TY_INFER_MASK);
            myOrigin = origin;
            myParent = new VarValue<>(null, 0);
        }

        @Nullable
        public Ty getOrigin() {
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

    public static class IntVar extends TyInfer implements Node {
        private NodeOrValue myParent;

        public IntVar() {
            super(KindUtil.HAS_TY_INFER_MASK);
            myParent = new VarValue<>(null, 0);
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

    public static class FloatVar extends TyInfer implements Node {
        private NodeOrValue myParent;

        public FloatVar() {
            super(KindUtil.HAS_TY_INFER_MASK);
            myParent = new VarValue<>(null, 0);
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
}
