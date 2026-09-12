/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.inlineTypeAlias.RsInlineTypeAliasProcessor;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.ide.utils.imports.RsImportHelper;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.RsTypeReference;
import org.rust.lang.core.resolve.ref.RsPathReferenceImpl;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.RsTypesUtil;
import consulo.localize.LocalizeValue;
import org.rust.lang.core.psi.ext.RsElement;

/** See also {@link RsInlineTypeAliasProcessor} */
public class SubstituteTypeAliasIntention extends RsElementBaseIntentionAction<SubstituteTypeAliasIntention.Context> {
    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.substitute.type.alias"));
        }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
        }

    public static class Context {
        private final RsPath myPath;
        private final RsTypeAlias myTypeAlias;
        private final RsTypeReference myTypeAliasReference;
        private final Substitution mySubstitution;

        public Context(@Nonnull RsPath path, @Nonnull RsTypeAlias typeAlias,
                       @Nonnull RsTypeReference typeAliasReference, @Nonnull Substitution substitution) {
            myPath = path;
            myTypeAlias = typeAlias;
            myTypeAliasReference = typeAliasReference;
            mySubstitution = substitution;
        }

        @Nonnull
        public RsPath getPath() {
            return myPath;
        }

        @Nonnull
        public RsTypeAlias getTypeAlias() {
            return myTypeAlias;
        }

        @Nonnull
        public RsTypeReference getTypeAliasReference() {
            return myTypeAliasReference;
        }

        @Nonnull
        public Substitution getSubstitution() {
            return mySubstitution;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
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
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Context ctx) {
        Substitution substitution = RsInlineTypeAliasProcessor.tryGetTypeAliasSubstitutionUsingParent(ctx.getPath(), ctx.getTypeAlias());
        if (substitution == null) {
            substitution = ctx.getSubstitution();
        }
        org.rust.lang.core.psi.ext.RsElement inlined = RsInlineTypeAliasProcessor.fillPathWithActualType(ctx.getPath(), ctx.getTypeAliasReference(), substitution);
        if (inlined == null) return;
        RsImportHelper.importTypeReferencesFromTy(inlined, RsTypesUtil.getRawType(ctx.getTypeAliasReference()));
    }
}
