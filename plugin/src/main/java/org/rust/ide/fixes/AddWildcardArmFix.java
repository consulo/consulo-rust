/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.utils.checkMatch.Pattern;
import org.rust.lang.core.psi.RsMatchArm;
import org.rust.lang.core.psi.RsMatchExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.Collections;
import java.util.List;
import consulo.localize.LocalizeValue;

public class AddWildcardArmFix extends AddRemainingArmsFix {
    
    
    public static final String NAME = RsBundle.message("intention.name.add.pattern");

    public AddWildcardArmFix(@Nonnull RsMatchExpr match) {
        super(match, Collections.emptyList());
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(NAME);
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return getFamilyName();
    }

    @Override
    public List<RsMatchArm> createNewArms(@Nonnull RsPsiFactory psiFactory, @Nonnull RsElement context) {
        return List.of(
            psiFactory.createMatchBody(List.of(Pattern.wild())).getMatchArmList().get(0)
        );
    }
}
