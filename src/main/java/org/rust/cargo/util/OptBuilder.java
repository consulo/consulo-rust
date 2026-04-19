/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

import java.util.List;

public interface OptBuilder {
    List<Opt> getResult();

    default void flag(String longName) {
        getResult().add(new Opt(longName));
    }

    default void opt(String longName, ArgCompleter argCompleter) {
        getResult().add(new Opt(longName, argCompleter));
    }
}
