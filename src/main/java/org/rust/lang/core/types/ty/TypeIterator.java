/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import java.util.*;

/**
 * Iterator that walks root and any types reachable from root, in depth-first order.
 */
public class TypeIterator implements Iterator<Ty> {
    private final Deque<Ty> myStack;

    public TypeIterator(Ty root) {
        myStack = new ArrayDeque<>();
        myStack.push(root);
    }

    @Override
    public boolean hasNext() {
        return !myStack.isEmpty();
    }

    @Override
    public Ty next() {
        Ty ty = myStack.pop();
        pushSubTypes(myStack, ty);
        return ty;
    }

    private static void pushSubTypes(Deque<Ty> stack, Ty parentTy) {
        if (parentTy instanceof TyAdt) {
            List<Ty> args = ((TyAdt) parentTy).getTypeArguments();
            for (int i = args.size() - 1; i >= 0; i--) stack.push(args.get(i));
        } else if (parentTy instanceof TyAnon || parentTy instanceof TyTraitObject || parentTy instanceof TyProjection) {
            List<Ty> types = new ArrayList<>(parentTy.getTypeParameterValues().getTypes());
            for (int i = types.size() - 1; i >= 0; i--) stack.push(types.get(i));
        } else if (parentTy instanceof TyArray) {
            stack.push(((TyArray) parentTy).getBase());
        } else if (parentTy instanceof TyPointer) {
            stack.push(((TyPointer) parentTy).getReferenced());
        } else if (parentTy instanceof TyReference) {
            stack.push(((TyReference) parentTy).getReferenced());
        } else if (parentTy instanceof TySlice) {
            stack.push(((TySlice) parentTy).getElementType());
        } else if (parentTy instanceof TyTuple) {
            List<Ty> types = ((TyTuple) parentTy).getTypes();
            for (int i = types.size() - 1; i >= 0; i--) stack.push(types.get(i));
        } else if (parentTy instanceof TyFunctionBase) {
            TyFunctionBase fn = (TyFunctionBase) parentTy;
            stack.push(fn.getRetType());
            List<Ty> params = fn.getParamTypes();
            for (int i = params.size() - 1; i >= 0; i--) stack.push(params.get(i));
        }
    }
}
