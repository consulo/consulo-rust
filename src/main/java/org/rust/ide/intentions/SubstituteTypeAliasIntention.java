/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.inlineTypeAlias.RsInlineTypeAliasProcessor;
import org.rust.ide.refactoring.inlineTypeAlias.InlineTypeAliasUtil;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.ide.utils.imports.RsImportHelper;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.RsTypeReference;
import org.rust.lang.core.resolve.ref.RsPathReferenceImpl;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.RsTypesUtil;

/** See also {@link RsInlineTypeAliasProcessor} */
public class SubstituteTypeAliasIntention extends RsElementBaseIntentionAction<SubstituteTypeAliasIntention.Context> {
    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.substitute.type.alias");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    public static class Context {
        private final RsPath myPath;
        private final RsTypeAlias myTypeAlias;
        private final RsTypeReference myTypeAliasReference;
        private final Substitution mySubstitution;

        public Context(@NotNull RsPath path, @NotNull RsTypeAlias typeAlias,
                       @NotNull RsTypeReference typeAliasReference, @NotNull Substitution substitution) {
            myPath = path;
            myTypeAlias = typeAlias;
            myTypeAliasReference = typeAliasReference;
            mySubstitution = substitution;
        }

        @NotNull
        public RsPath getPath() {
            return myPath;
        }

        @NotNull
        public RsTypeAlias getTypeAlias() {
            return myTypeAlias;
        }

        @NotNull
        public RsTypeReference getTypeAliasReference() {
            return myTypeAliasReference;
        }

        @NotNull
        public Substitution getSubstitution() {
            return mySubstitution;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        RsPath path = PsiTreeUtil.getParentOfType(element, RsPath.class);
        if (path == null) return null;
        if (path.getReference() == null) return null;
        BoundElement<?> target = RsPathReferenceImpl.advancedResolveTypeAliasToImpl(path.getReference());
        if (target == null) return null;
        PsiElement resolved = target.getTypedElement();
        if (!(resolved instanceof RsTypeAlias)) return null;
        RsTypeAlias typeAlias = (RsTypeAlias) resolved;
        RsTypeReference typeRef = typeAlias.getTypeReference();
        if (typeRef == null) return null;

        if (!PsiModificationUtil.canReplace(path)) return null;

        return new Context(path, typeAlias, typeRef, target.getSubst());
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Context ctx) {
        Substitution substitution = RsInlineTypeAliasProcessor.tryGetTypeAliasSubstitutionUsingParent(ctx.getPath(), ctx.getTypeAlias());
        if (substitution == null) {
            substitution = ctx.getSubstitution();
        }
        org.rust.lang.core.psi.ext.RsElement inlined = RsInlineTypeAliasProcessor.fillPathWithActualType(ctx.getPath(), ctx.getTypeAliasReference(), substitution);
        if (inlined == null) return;
        RsImportHelper.importTypeReferencesFromTy(inlined, RsTypesUtil.getRawType(ctx.getTypeAliasReference()));
    }
}
