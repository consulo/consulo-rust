/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

import java.util.Collections;
import java.util.List;

public record StdLibInfo(String name, StdLibType type, List<String> dependencies) {
    public StdLibInfo(String name, StdLibType type) {
        this(name, type, Collections.emptyList());
    }
}
