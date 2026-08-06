/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.presentation.TypeRendering;
import org.rust.ide.utils.PsiInsertionPlace;
import org.rust.ide.utils.imports.RsImportHelper;
import org.rust.ide.utils.imports.TypeReferencesInfo;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.psi.ext.RsBindingModeKind;
import org.rust.lang.core.psi.ext.RsPatBindingUtil;
import org.rust.lang.core.types.consts.CtInferVar;
import org.rust.lang.core.types.consts.CtUnevaluated;
import org.rust.lang.core.types.consts.CtUnknown;
import org.rust.lang.core.types.ty.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.infer.FoldUtil;
import org.rust.lang.core.types.infer.TypeFoldable;
import org.rust.lang.core.types.infer.TypeVisitor;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.doc.psi.RsQualifiedName;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

public class SpecifyTypeExplicitlyIntention extends RsElementBaseIntentionAction<SpecifyTypeExplicitlyIntention.Context> {

    @SuppressWarnings("unchecked")
    private static <T> boolean containsConstOfClass(@Nonnull TypeFoldable<T> foldable, @Nonnull Class<?>... classes) {
        List<Class<?>> classList = Arrays.asList(classes);
        return foldable.visitWith(new TypeVisitor() {
            @Override
            public boolean visitConst(@Nonnull Const aConst) {
                for (Class<?> clazz : classList) {
                    if (clazz.isInstance(aConst)) return true;
                }
                return aConst.superVisitWith(this);
            }
        });
    }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.specify.type.explicitly"));
        }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.specify.type.explicitly"));
        }

    public static class Context {
        private final Ty myType;
        private final RsLetDecl myLetDecl;
        private final PsiInsertionPlace myPlace;

        public Context(@Nonnull Ty type, @Nonnull RsLetDecl letDecl, @Nonnull PsiInsertionPlace place) {
            myType = type;
            myLetDecl = letDecl;
            myPlace = place;
        }

        @Nonnull
        public Ty getType() {
            return myType;
        }

        @Nonnull
        public RsLetDecl getLetDecl() {
            return myLetDecl;
        }

        @Nonnull
        public PsiInsertionPlace getPlace() {
            return myPlace;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
        RsLetDecl letDecl = PsiElementExt.ancestorStrict(element, RsLetDecl.class);
        if (letDecl == null) return null;
        if (letDecl.getTypeReference() != null) return null;
        RsExpr initializer = letDecl.getExpr();
        if (initializer != null && PsiElementExt.getStartOffset(element) >= PsiElementExt.getStartOffset(initializer) - 1) return null;
        RsPat pat = letDecl.getPat();
        if (pat == null) return null;
        Ty patType = RsTypesUtil.getType(pat);
        if (FoldUtil.containsTyOfClass(patType, TyUnknown.class, TyInfer.class, TyAnon.class)
            || containsConstOfClass(patType, CtUnknown.class, CtInferVar.class, CtUnevaluated.class)) {
            return null;
        }

        // let ref x = 1; // `i32` should be inserted instead of `&i32`
        Ty type;
        if (pat instanceof RsPatIdent && RsPatBindingUtil.getKind(((RsPatIdent) pat).getPatBinding()) instanceof RsBindingModeKind.BindByReference && patType instanceof TyReference) {
            type = ((TyReference) patType).getReferenced();
        } else {
            type = patType;
        }

        PsiInsertionPlace place = PsiInsertionPlace.after(pat);
        if (place == null) return null;

        return new Context(type, letDecl, place);
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Context ctx) {
        RsPsiFactory factory = new RsPsiFactory(project);
        RsLetDecl letDecl = ctx.getLetDecl();
        TypeReferencesInfo refsInfo =
            RsImportHelper.getTypeReferencesInfoFromTys(letDecl, ctx.getType());
        Set<RsQualifiedNamedElement> toImport = refsInfo.getToImport();
        Set<RsQualifiedNamedElement> toQualify = refsInfo.getToQualify();

        RsTypeReference createdType = factory.createType(TypeRendering.renderInsertionSafe(ctx.getType(), letDecl, Integer.MAX_VALUE, toQualify, true, false, true, true));

        ctx.getPlace().insertMultiple(factory.createColon(), createdType);

        RsImportHelper.importElements(letDecl, toImport);
    }
}
