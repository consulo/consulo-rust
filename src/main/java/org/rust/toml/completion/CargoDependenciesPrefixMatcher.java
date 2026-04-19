/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;

import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.psi.codeStyle.MinusculeMatcher;
import com.intellij.psi.codeStyle.NameUtil;
import org.jetbrains.annotations.NotNull;

public class CargoDependenciesPrefixMatcher extends PrefixMatcher {
    private final String myNormalizedPrefix;
    private final MinusculeMatcher myMinusculeMatcher;

    public CargoDependenciesPrefixMatcher(@NotNull String prefix) {
        super(prefix);
        myNormalizedPrefix = normalize(prefix);
        myMinusculeMatcher = NameUtil.buildMatcher(myNormalizedPrefix).withSeparators("_").build();
    }

    @Override
    public boolean prefixMatches(@NotNull String name) {
        String normalizedName = normalize(name);
        return myMinusculeMatcher.matches(normalizedName);
    }

    @NotNull
    @Override
    public PrefixMatcher cloneWithPrefix(@NotNull String prefix) {
        return new CargoDependenciesPrefixMatcher(prefix);
    }

    @NotNull
    private String normalize(@NotNull String string) {
        return string.replace('-', '_');
    }
}
