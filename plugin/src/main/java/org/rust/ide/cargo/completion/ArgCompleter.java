/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.cargo.completion;

import consulo.language.editor.completion.lookup.LookupElement;

import java.util.List;
import java.util.function.Function;
import org.rust.cargo.util.Context;

@FunctionalInterface
public interface ArgCompleter extends Function<Context, List<LookupElement>> {
}
