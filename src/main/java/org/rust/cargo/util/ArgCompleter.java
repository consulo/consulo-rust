/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

import com.intellij.codeInsight.lookup.LookupElement;

import java.util.List;
import java.util.function.Function;

@FunctionalInterface
public interface ArgCompleter extends Function<Context, List<LookupElement>> {
}
