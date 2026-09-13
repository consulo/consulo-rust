/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.match;

import jakarta.annotation.Nonnull;

public class CheckMatchException extends RuntimeException {
    public CheckMatchException(@Nonnull String message) {
        super(message);
    }
}
