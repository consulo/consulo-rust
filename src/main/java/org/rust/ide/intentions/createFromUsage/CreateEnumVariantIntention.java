/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.createFromUsage;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.RsElementBaseIntentionAction;
import org.rust.ide.utils.PsiInsertionPlace;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.ext.RsEnumItemUtil;
import org.rust.lang.core.psi.ext.RsPathUtil;

public class CreateEnumVariantIntention extends RsElementBaseIntentionAction<CreateEnumVariantIntention.Context> {
    @Override
    @NotNull
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.create.enum.variant");
    }

    public static class Context {
        @NotNull
        public final RsPath path;
        @NotNull
        public final RsEnumItem enumItem;
        @NotNull
        public final String name;
        @NotNull
        public final PsiInsertionPlace place;
        @Nullable
        public final PsiInsertionPlace commaPlace;

        public Context(@NotNull RsPath path, @NotNull RsEnumItem enumItem, @NotNull String name,
                        @NotNull PsiInsertionPlace place, @Nullable PsiInsertionPlace commaPlace) {
            this.path = path;
            this.enumItem = enumItem;
            this.name = name;
            this.place = place;
            this.commaPlace = commaPlace;
        }
    }

    @Override
    @Nullable
    public Context findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
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

        setText(RsBundle.message("intention.name.create.enum.variant", name));
        return new Context(path, enumItem, name, place, commaPlace);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Context ctx) {
        RsPsiFactory factory = new RsPsiFactory(project);

        String fields = generateFields(ctx.path);
        RsEnumVariant variant = factory.createEnumVariant(ctx.name + fields);

        if (ctx.commaPlace != null) {
            ctx.commaPlace.insert(factory.createComma());
        }
        ctx.place.insertMultiple(variant, factory.createComma(), factory.createNewline());
    }

    @NotNull
    private String generateFields(@NotNull RsPath path) {
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

    private static boolean hasTrailingComma(@NotNull RsEnumVariant variant) {
        PsiElement next = RsElementUtil.getNextNonCommentSibling(variant);
        return next != null && next.getNode().getElementType() == RsElementTypes.COMMA;
    }

    @Override
    @NotNull
    public IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }
}
