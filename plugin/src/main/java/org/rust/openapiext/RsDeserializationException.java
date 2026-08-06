/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.fasterxml.jackson.core.JacksonException;
import jakarta.annotation.Nonnull;

public class RsDeserializationException extends RsProcessExecutionOrDeserializationException {
    public RsDeserializationException(@Nonnull JacksonException cause) {
        super(cause);
    }
}
