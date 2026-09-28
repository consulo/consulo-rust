/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.type;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.inlay.DeclarativePresentationTreeBuilder;
import consulo.language.editor.inlay.InlayGroup;
import consulo.language.editor.inlay.chain.AbstractDeclarativeCallChainProvider;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.localize.LocalizeValue;
import consulo.project.DumbService;
import consulo.project.Project;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.RsDotExpr;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsMethodCall;
import org.rust.lang.core.psi.RsParenExpr;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsTryExpr;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.ext.impl.RsCodeStatus;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import org.rust.lang.core.psi.ext.impl.RsGenericDeclarationUtil;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.TraitRef;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyAnon;
import org.rust.lang.core.types.ty.TyUnknown;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@ExtensionImpl
public class RsChainMethodTypeHintsProvider
    extends AbstractDeclarativeCallChainProvider<RsDotExpr, RsChainMethodTypeHintsProvider.ChainType, RsChainMethodTypeHintsProvider.Context> {

    public static final String PROVIDER_ID = "rust.method.chains";

    private final RsTypeHintsFactory myFactory = new RsTypeHintsFactory(true);

    @Override
    public Language getLanguage() {
        return RsLanguage.INSTANCE;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.of(RsBundle.message("settings.rust.inlay.hints.title.method.chains"));
    }

    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.of(RsBundle.message("settings.rust.inlay.hints.title.method.chains"));
    }

    @Override
    public LocalizeValue getPreviewFileText() {
        return LocalizeValue.empty();
    }

    @Override
    public InlayGroup getGroup() {
        return InlayGroup.METHOD_CHAINS_GROUP;
    }

    @Override
    protected boolean isAvailable(PsiFile file, Editor editor) {
        return file instanceof RsFile && !DumbService.isDumb(file.getProject());
    }

    @Override
    public Class<RsDotExpr> getDotQualifiedClass() {
        return RsDotExpr.class;
    }

    @Override
    protected Context getTypeComputationContext(RsDotExpr topmostDotQualifiedExpression) {
        PsiFile file = topmostDotQualifiedExpression.getContainingFile();
        Crate crate = file instanceof RsFile rsFile ? rsFile.getCrate() : null;
        ImplLookup lookup = file instanceof RsFile rsFile ? RsTypesUtil.getImplLookup(rsFile) : null;

        BoundElement<RsTraitItem> iterator = null;
        if (file instanceof RsFile rsFile) {
            KnownItems items = RsTypesUtil.getKnownItems(rsFile);
            RsTraitItem iteratorTrait = items.getIterator();
            if (iteratorTrait != null) {
                iterator = RsGenericDeclarationUtil.withDefaultSubst(iteratorTrait);
            }
        }
        return new Context(crate, lookup, iterator);
    }

    @Override
    protected ChainType getType(PsiElement element, Context context) {
        if (!(element instanceof RsExpr expr)) return null;
        if (RsElementUtil.getCodeStatus(expr, context.crate) == RsCodeStatus.CFG_DISABLED) return null;

        Ty type = RsTypesUtil.getType(expr);
        if (type == TyUnknown.INSTANCE) return null;
        return new ChainType(normalize(type, context));
    }

    private static Ty normalize(Ty type, Context context) {
        if (context.lookup == null || context.iterator == null) return type;
        Map<RsTypeAlias, Ty> assoc = context.lookup.selectAllProjectionsStrict(new TraitRef(type, context.iterator));
        if (assoc == null) return type;
        return new TyAnon(null, Collections.singletonList(
            new BoundElement<>(context.iterator.getTypedElement(), context.iterator.getSubst(), assoc)));
    }

    @Override
    protected void buildTree(ChainType type, PsiElement expression, Project project, Context context,
                             DeclarativePresentationTreeBuilder treeBuilder) {
        myFactory.typeHint(type.ty, treeBuilder);
    }

    @Override
    protected PsiElement getReceiver(RsDotExpr expr) {
        return expr.getMethodCall() != null ? expr.getExpr() : null;
    }

    @Override
    protected RsDotExpr getParentDotQualifiedExpression(RsDotExpr expr) {
        PsiElement parent = expr.getParent();
        while (parent instanceof RsParenExpr || parent instanceof RsTryExpr) {
            parent = parent.getParent();
        }
        return parent instanceof RsDotExpr dot ? dot : null;
    }

    @Override
    protected PsiElement skipParenthesesAndPostfixOperatorsDown(PsiElement element) {
        PsiElement current = element;
        while (true) {
            if (current instanceof RsParenExpr paren) {
                current = paren.getExpr();
            }
            else if (current instanceof RsTryExpr tryExpr) {
                current = tryExpr.getExpr();
            }
            else {
                break;
            }
        }
        return current;
    }

    @Override
    protected boolean isChainUnacceptable(List<ExpressionWithType<ChainType>> chain) {
        for (ExpressionWithType<ChainType> link : chain) {
            if (!isLastOnLine(link.expression)) return true;
        }
        return false;
    }

    private static boolean isLastOnLine(PsiElement element) {
        PsiElement sibling = element.getNextSibling();
        if (sibling instanceof PsiWhiteSpace) {
            return sibling.textContains('\n') || isLastOnLine(sibling);
        }
        if (sibling instanceof PsiComment) {
            return isLastOnLine(sibling);
        }
        return false;
    }

    protected static class Context {
        private final @Nullable Crate crate;
        private final @Nullable ImplLookup lookup;
        private final @Nullable BoundElement<RsTraitItem> iterator;

        Context(@Nullable Crate crate, @Nullable ImplLookup lookup, @Nullable BoundElement<RsTraitItem> iterator) {
            this.crate = crate;
            this.lookup = lookup;
            this.iterator = iterator;
        }
    }

    protected static class ChainType {
        private final Ty ty;

        ChainType(Ty ty) {
            this.ty = ty;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ChainType that && ty.isEquivalentTo(that.ty);
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }
}
