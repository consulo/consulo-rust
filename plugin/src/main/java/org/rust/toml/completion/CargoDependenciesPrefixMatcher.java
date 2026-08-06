/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;

import consulo.application.util.matcher.PrefixMatcher;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.application.util.matcher.NameUtil;
import jakarta.annotation.Nonnull;

public class CargoDependenciesPrefixMatcher extends PrefixMatcher {
    private final String myNormalizedPrefix;
    private final MinusculeMatcher myMinusculeMatcher;

    public CargoDependenciesPrefixMatcher(@Nonnull String prefix) {
        super(prefix);
        myNormalizedPrefix = normalize(prefix);
        myMinusculeMatcher = NameUtil.buildMatcher(myNormalizedPrefix).withSeparators("_").build();
    }

    @Override
    public boolean prefixMatches(@Nonnull String name) {
        String normalizedName = normalize(name);
        return myMinusculeMatcher.matches(normalizedName);
    }

    @Nonnull
    @Override
    public PrefixMatcher cloneWithPrefix(@Nonnull String prefix) {
        return new CargoDependenciesPrefixMatcher(prefix);
    }

    @Nonnull
    private String normalize(@Nonnull String string) {
        return string.replace('-', '_');
    }
}
