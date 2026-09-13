/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.implementMembers;

import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import org.rust.lang.core.psi.ext.impl.RsConstantUtil;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.presentation.ImportingPsiRenderer;
import org.rust.lang.core.presentation.PsiRenderingOptions;
import org.rust.lang.core.presentation.RsPsiRendererUtil;
import org.rust.lang.core.imports.ImportCandidate;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.ty.TyUnknown;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.rust.lang.core.types.infer.FoldUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.ide.utils.RsDefaultValueBuilder;
import org.rust.lang.core.psi.impl.*;

public class MembersGenerator {
    @Nonnull
    private final RsPsiFactory myFactory;
    @Nonnull
    private final BoundElement<RsTraitItem> myTrait;
    @Nonnull
    private final ImportingPsiRenderer myRenderer;

    public MembersGenerator(
        @Nonnull RsPsiFactory factory,
        @Nonnull RsImplItem impl,
        @Nonnull BoundElement<RsTraitItem> trait
    ) {
        myFactory = factory;
        myTrait = trait;
        myRenderer = new ImportingPsiRenderer(
            new PsiRenderingOptions(false),
            Collections.singletonList(trait.getSubst()),
            impl.getMembers()
        );
    }

    @Nonnull
    public Set<ImportCandidate> getItemsToImport() {
        return myRenderer.getItemsToImport();
    }

    @Nonnull
    public RsMembers createTraitMembers(@Nonnull Collection<RsAbstractable> members) {
        String body = members.stream()
            .map(m -> "    " + renderAbstractable(m))
            .collect(Collectors.joining("\n"));
        return myFactory.createMembers(body);
    }

    @Nonnull
    public String renderAbstractable(@Nonnull RsAbstractable element) {
        Substitution subst = myTrait.getSubst();
        if (element instanceof RsConstant) {
            RsConstant constant = (RsConstant) element;
            org.rust.lang.core.types.ty.Ty type = constant.getTypeReference() != null
                ? org.rust.lang.core.types.infer.FoldUtil.substitute(RsTypesUtil.getNormType(constant.getTypeReference()), subst)
                : TyUnknown.INSTANCE;
            String initialValue = new RsDefaultValueBuilder(
                KnownItems.getKnownItems(element),
                RsElementUtil.containingMod(element),
                myFactory,
                true
            ).buildFor(type, Collections.emptyMap()).getText();
            String typeText = constant.getTypeReference() != null
                ? RsPsiRendererUtil.renderTypeReference(myRenderer, constant.getTypeReference())
                : "_";
            return "const " + RsConstantUtil.getNameLikeElement(constant).getText() + ": " + typeText + " = " + initialValue + ";";
        } else if (element instanceof RsTypeAlias) {
            return RsPsiRendererUtil.renderTypeAliasSignature(myRenderer, (RsTypeAlias) element, false) + " = ();";
        } else if (element instanceof RsFunction) {
            return RsPsiRendererUtil.renderFunctionSignature(myRenderer, (RsFunction) element) + " {\n        todo!()\n    }";
        } else {
            throw new IllegalStateException("Unknown trait member");
        }
    }
}
