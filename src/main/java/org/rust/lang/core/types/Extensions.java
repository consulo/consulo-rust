/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValuesManager;
import org.rust.lang.core.dfa.ControlFlowGraph;
import org.rust.lang.core.dfa.liveness.Liveness;
import org.rust.lang.core.mir.borrowck.MirBorrowCheckResult;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.infer.*;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyTypeParameter;
import org.rust.lang.core.types.ty.TyUnknown;
public final class Extensions {
    private Extensions() {}

}
