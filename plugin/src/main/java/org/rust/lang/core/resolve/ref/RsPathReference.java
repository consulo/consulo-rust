/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.RsPathResolveResult;
import org.rust.lang.core.types.BoundElement;

import java.util.ArrayList;
import java.util.List;

public interface RsPathReference extends RsReference {

    @Nonnull
    @Override
    RsPath getElement();

    @Nullable
    default RsElement resolveIfVisible() {
        List<RsElement> visible = multiResolveIfVisible();
        return visible.size() == 1 ? visible.get(0) : null;
    }

    @Nonnull
    default List<RsElement> multiResolveIfVisible() {
        return multiResolve();
    }

    @Nonnull
    default List<RsPathResolveResult<RsElement>> rawMultiResolve() {
        List<RsElement> resolved = multiResolve();
        List<RsPathResolveResult<RsElement>> results = new ArrayList<>(resolved.size());
        for (RsElement element : resolved) {
            results.add(new RsPathResolveResult<>(element, true));
        }
        return results;
    }

    @Nullable
    default BoundElement<RsElement> advancedResolve() {
        RsElement resolved = resolve();
        return resolved != null ? new BoundElement<>(resolved) : null;
    }
}
