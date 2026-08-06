/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.impl;

public class CargoMetadataException extends IllegalStateException {
    public CargoMetadataException(String message) {
        super(message);
    }
}
