/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

/**
 * An entity in the Rust type system, which can be one of several kinds (only types, lifetimes and constants for now).
 */
public interface Kind {
    int getFlags();
}
