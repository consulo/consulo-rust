/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.createFromUsage;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.RsElementBaseIntentionAction;
import org.rust.ide.utils.PsiInsertionPlace;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.ext.impl.RsEnumItemUtil;
import org.rust.lang.core.psi.ext.impl.RsPathUtil;
import consulo.localize.LocalizeValue;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.psi.ext.impl.*;

public class CreateEnumVariantIntention extends RsElementBaseIntentionAction<CreateEnumVariantIntention.Context> {
    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.create.enum.variant"));
        }

    public static class Context {
        @Nonnull
        public final RsPath path;
        @Nonnull
        public final RsEnumItem enumItem;
        @Nonnull
        public final String name;
        @Nonnull
        public final PsiInsertionPlace place;
        @Nullable
        public final PsiInsertionPlace commaPlace;

        public Context(@Nonnull RsPath path, @Nonnull RsEnumItem enumItem, @Nonnull String name,
                        @Nonnull PsiInsertionPlace place, @Nullable PsiInsertionPlace commaPlace) {
            this.path = path;
            this.enumItem = enumItem;
            this.name = name;
            this.place = place;
            this.commaPlace = commaPlace;
        }
    }

    @Override
    @Nullable
    public Context findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
        RsPath path = RsElementUtil.contextStrict(element, RsPath.class);
        if (path == null || path.getIdentifier() != element) return null;
        if (path.getContext() instanceof RsPath) return null;
        if (RsPathUtil.getResolveStatus(path) != PathResolveStatus.UNRESOLVED) return null;
        String name = path.getReferenceName();
        if (name == null) return null;
        if (Character.isLowerCase(name.charAt(0))) return null;

        RsPath qualifier = RsPathUtil.getQualifier(path);
        if (qualifier == null) return null;
        PsiElement resolved = qualifier.getReference() != null ? qualifier.getReference().resolve() : null;
        if (!(resolved instanceof RsEnumItem)) return null;
        RsEnumItem enumItem = (RsEnumItem) resolved;

        for (RsEnumVariant variant : RsEnumItemUtil.getVariants(enumItem)) {
            if (name.equals(variant.getName())) return null;
        }

        java.util.List<RsEnumVariant> variants = RsEnumItemUtil.getVariants(enumItem);
        RsEnumVariant lastVariant = !variants.isEmpty() ? variants.get(variants.size() - 1) : null;
        PsiInsertionPlace commaPlace = null;
        if (lastVariant != null && !hasTrailingComma(lastVariant)) {
            commaPlace = PsiInsertionPlace.after(lastVariant);
            if (commaPlace == null) return null;
        }

        RsEnumBody enumBody = enumItem.getEnumBody();
        if (enumBody == null) return null;
        PsiElement rbrace = enumBody.getRbrace();
        if (rbrace == null) return null;
        PsiInsertionPlace place = PsiInsertionPlace.before(rbrace);
        if (place == null) return null;

        setText(consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.create.enum.variant", name)));
        return new Context(path, enumItem, name, place, commaPlace);
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Context ctx) {
        RsPsiFactory factory = new RsPsiFactory(project);

        String fields = generateFields(ctx.path);
        RsEnumVariant variant = factory.createEnumVariant(ctx.name + fields);

        if (ctx.commaPlace != null) {
            ctx.commaPlace.insert(factory.createComma());
        }
        ctx.place.insertMultiple(variant, factory.createComma(), factory.createNewline());
    }

    @Nonnull
    private String generateFields(@Nonnull RsPath path) {
        PsiElement context = path.getContext();
        if (context instanceof RsStructLiteral) {
            return CreateStructIntention.generateFields((RsStructLiteral) context, "").replace('\n', ' ');
        }
        PsiElement context2 = context != null ? context.getContext() : null;
        if (context instanceof RsPathExpr && context2 instanceof RsCallExpr) {
            return CreateTupleStructIntention.generateFields((RsCallExpr) context2, "");
        }
        return "";
    }

    private static boolean hasTrailingComma(@Nonnull RsEnumVariant variant) {
        PsiElement next = RsElementUtil.getNextNonCommentSibling(variant);
        return next != null && next.getNode().getElementType() == RsElementTypes.COMMA;
    }

    @Nonnull
    public IntentionPreviewInfo generatePreview(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }
}
