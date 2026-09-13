/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.workspace.CargoWorkspace;

import java.util.Collection;
import java.util.List;

public record Context(
    Collection<CargoProject> projects,
    CargoWorkspace currentWorkspace,
    List<String> commandLinePrefix
) {
}
