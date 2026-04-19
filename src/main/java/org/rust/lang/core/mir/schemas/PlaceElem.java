/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.rust.lang.core.types.ty.Ty;

/**
 * Since Java does not support typealiases, this is an interface that serves as a marker.
 * In practice, all MirProjectionElem instances are used as PlaceElem.
 */
public interface PlaceElem {
    /**
     * Lifts a PlaceElem to an abstract element (erasing the type parameter).
     */
    @SuppressWarnings("rawtypes")
    default MirProjectionElem lift() {
        return (MirProjectionElem) this;
    }
}
