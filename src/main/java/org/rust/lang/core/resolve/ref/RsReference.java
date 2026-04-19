/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import com.intellij.psi.PsiPolyVariantReference;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.List;

public interface RsReference extends PsiPolyVariantReference {

    @Override
    RsElement getElement();

    @Nullable
    @Override
    RsElement resolve();

    List<RsElement> multiResolve();
}
