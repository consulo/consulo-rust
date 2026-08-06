/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

/**
 * Base sealed class for process execution or deserialization exceptions.
 */
public abstract class RsProcessExecutionOrDeserializationException extends RuntimeException {
    protected RsProcessExecutionOrDeserializationException(Throwable cause) {
        super(cause);
    }

    protected RsProcessExecutionOrDeserializationException(String message) {
        super(message);
    }
}
