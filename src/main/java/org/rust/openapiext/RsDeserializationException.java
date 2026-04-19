/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.fasterxml.jackson.core.JacksonException;
import org.jetbrains.annotations.NotNull;

public class RsDeserializationException extends RsProcessExecutionOrDeserializationException {
    public RsDeserializationException(@NotNull JacksonException cause) {
        super(cause);
    }
}
