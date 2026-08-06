/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace;

/**
 * Defines a reason a package is in a project.
 */
public enum PackageOrigin {
    /**
     * The package comes from the standard library.
     */
    STDLIB,

    /**
     * The package is a part of our workspace.
     */
    WORKSPACE,

    /**
     * External dependency of {@link #WORKSPACE} or other {@link #DEPENDENCY} package
     */
    DEPENDENCY,

    /**
     * External dependency of {@link #STDLIB} or other {@link #STDLIB_DEPENDENCY} package
     */
    STDLIB_DEPENDENCY
}
