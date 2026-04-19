/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.utils.checkMatch.Pattern;
import org.rust.lang.core.psi.RsMatchArm;
import org.rust.lang.core.psi.RsMatchExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.Collections;
import java.util.List;

public class AddWildcardArmFix extends AddRemainingArmsFix {
    @IntentionName
    @IntentionFamilyName
    public static final String NAME = RsBundle.message("intention.name.add.pattern");

    public AddWildcardArmFix(@NotNull RsMatchExpr match) {
        super(match, Collections.emptyList());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return NAME;
    }

    @NotNull
    @Override
    public String getText() {
        return getFamilyName();
    }

    @Override
    public List<RsMatchArm> createNewArms(@NotNull RsPsiFactory psiFactory, @NotNull RsElement context) {
        return List.of(
            psiFactory.createMatchBody(List.of(Pattern.wild())).getMatchArmList().get(0)
        );
    }
}
