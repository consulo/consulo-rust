/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.move;

/**
 *
 * In Java, these have been split into:
 * - {@link DropFlagState} - the enum
 * - {@link DropFlagEffectUtil} - the static utility methods (dropFlagEffectsForLocation,
 *   forLocationInits, onAllChildrenBits, dropFlagEffectsForFunctionEntry)
 *
 * This file exists as a reference/index.
 */
public final class DropFlagEffect {
    private DropFlagEffect() {
    }
}
