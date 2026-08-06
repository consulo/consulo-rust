/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

import java.util.List;

public record ParsedCargoArgs(List<String> commandArguments, List<String> executableArguments) {
}
