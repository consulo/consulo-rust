/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;



public sealed class DownloadResult<T> permits DownloadResult.Ok, DownloadResult.Err {

    public static final class Ok<T> extends DownloadResult<T> {
        private final T value;

        public Ok(T value) {
            this.value = value;
        }

        public T getValue() {
            return value;
        }
    }

    public static final class Err<T> extends DownloadResult<T> {
        
        private final String error;

        public Err( String error) {
            this.error = error;
        }

        
        public String getError() {
            return error;
        }
    }
}
