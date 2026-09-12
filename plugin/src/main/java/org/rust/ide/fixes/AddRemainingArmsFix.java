/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.editor.inspection.FileModifier.SafeFieldForPreview;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.utils.PsiInsertionPlace;
import org.rust.ide.utils.imports.ImportBridge;
import org.rust.ide.utils.checkMatch.Pattern;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.RsTypesUtil;

import java.util.Collections;
import java.util.List;
import consulo.localize.LocalizeValue;

public class AddRemainingArmsFix extends RsQuickFixBase<RsMatchExpr> {
    @SafeFieldForPreview
    private final List<Pattern> patterns;

    
    
    public static final String NAME = RsBundle.message("intention.name.add.remaining.patterns");

    public AddRemainingArmsFix(@Nonnull RsMatchExpr match, @Nonnull List<Pattern> patterns) {
        super(match);
        this.patterns = patterns;
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
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsMatchExpr element) {
        RsExpr expr = element.getExpr();
        if (expr == null) return;
        ArmsInsertionPlace place = findArmsInsertionPlaceIn(element);
        if (place == null) return;
        invoke(project, element, expr, place);
    }

    public void invoke(@Nonnull Project project, @Nonnull RsMatchExpr match, @Nonnull RsExpr expr, @Nonnull ArmsInsertionPlace place) {
        RsPsiFactory rsPsiFactory = new RsPsiFactory(project);
        List<RsMatchArm> newArms = createNewArms(rsPsiFactory, match);
        if (place instanceof ArmsInsertionPlace.AsNewBody) {
            ArmsInsertionPlace.AsNewBody asNewBody = (ArmsInsertionPlace.AsNewBody) place;
            RsMatchBody newMatchBody = rsPsiFactory.createMatchBody(Collections.emptyList());
            for (RsMatchArm arm : newArms) {
                newMatchBody.addBefore(arm, newMatchBody.getRbrace());
            }
            asNewBody.placeForBody.insert(newMatchBody);
        } else if (place instanceof ArmsInsertionPlace.ToExistingBody) {
            ArmsInsertionPlace.ToExistingBody toExisting = (ArmsInsertionPlace.ToExistingBody) place;
            if (toExisting.placeForComma != null) {
                toExisting.placeForComma.insert(rsPsiFactory.createComma());
            }
            toExisting.placeForArms.insertMultiple(newArms);
        }
        ImportBridge.importTypeReferencesFromTy(match, RsTypesUtil.getType(expr));
    }

    public List<RsMatchArm> createNewArms(@Nonnull RsPsiFactory psiFactory, @Nonnull RsElement context) {
        return psiFactory.createMatchBody(patterns).getMatchArmList();
    }

    public static abstract class ArmsInsertionPlace {
        public static class ToExistingBody extends ArmsInsertionPlace {
            @Nullable public final PsiInsertionPlace placeForComma;
            @Nonnull public final PsiInsertionPlace placeForArms;

            public ToExistingBody(@Nullable PsiInsertionPlace placeForComma, @Nonnull PsiInsertionPlace placeForArms) {
                this.placeForComma = placeForComma;
                this.placeForArms = placeForArms;
            }
        }

        public static class AsNewBody extends ArmsInsertionPlace {
            @Nonnull public final PsiInsertionPlace placeForBody;

            public AsNewBody(@Nonnull PsiInsertionPlace placeForBody) {
                this.placeForBody = placeForBody;
            }
        }
    }

    @Nullable
    public static ArmsInsertionPlace findArmsInsertionPlaceIn(@Nonnull RsMatchExpr match) {
        RsExpr expr = match.getExpr();
        if (expr == null) return null;
        RsMatchBody body = match.getMatchBody();
        if (body == null) {
            PsiInsertionPlace place = PsiInsertionPlace.after(expr);
            if (place == null) return null;
            return new ArmsInsertionPlace.AsNewBody(place);
        } else {
            List<RsMatchArm> arms = body.getMatchArmList();
            RsMatchArm lastMatchArm = arms.isEmpty() ? null : arms.get(arms.size() - 1);
            PsiInsertionPlace placeForComma = null;
            if (lastMatchArm != null && !(lastMatchArm.getExpr() instanceof RsBlockExpr) && lastMatchArm.getComma() == null) {
                placeForComma = PsiInsertionPlace.afterLastChildIn(lastMatchArm);
                if (placeForComma == null) return null;
            }
            PsiElement rbrace = body.getRbrace();
            PsiInsertionPlace placeForArms;
            if (rbrace != null) {
                placeForArms = PsiInsertionPlace.before(rbrace);
            } else {
                placeForArms = PsiInsertionPlace.afterLastChildIn(body);
            }
            if (placeForArms == null) return null;
            return new ArmsInsertionPlace.ToExistingBody(placeForComma, placeForArms);
        }
    }
}
