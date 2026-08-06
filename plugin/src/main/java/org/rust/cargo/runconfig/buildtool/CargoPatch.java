/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool;

import org.rust.cargo.toolchain.CargoCommandLine;

import java.util.function.Function;

/**
 * A functional interface representing a patch/transformation on a CargoCommandLine.
 */
@FunctionalInterface
public interface CargoPatch extends Function<CargoCommandLine, CargoCommandLine> {
}
