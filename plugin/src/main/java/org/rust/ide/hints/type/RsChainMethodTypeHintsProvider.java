/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.type;
import com.intellij.codeInsight.hints.InlayProviderDisablingAction;
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector;
import consulo.language.editor.inlay.InlayGroup;

import consulo.language.editor.inlay.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.codeInsight.hints.SettingsKey;
import com.intellij.codeInsight.hints.InlayHintsProvider;
import com.intellij.codeInsight.hints.InlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.ImmediateConfigurable;
import com.intellij.codeInsight.hints.ImmediateConfigurable.Case;
import com.intellij.codeInsight.hints.InlayHintsProvider.ChangeListener;
import consulo.language.editor.inlay.InlayPresentation;
import consulo.ide.impl.idea.codeInsight.hints.presentation.InsetPresentation;
import com.intellij.codeInsight.hints.presentation.MenuOnClickPresentation;
import consulo.codeEditor.Editor;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.application.util.registry.Registry;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.macros.MacroExpansionExtUtil;
import org.rust.lang.core.psi.RsDotExpr;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsMethodCall;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.RsCodeStatus;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.TraitRef;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyAnon;
import org.rust.lang.core.types.ty.TyUnknown;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.ext.RsGenericDeclarationUtil;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.psi.ext.RsMethodCallUtil;

@SuppressWarnings("UnstableApiUsage")
public class RsChainMethodTypeHintsProvider implements InlayHintsProvider<RsChainMethodTypeHintsProvider.Settings> {

    public static final SettingsKey<Settings> KEY = new SettingsKey<>("chain-method.hints");

    @Nonnull
    @Override
    public SettingsKey<Settings> getKey() {
        return KEY;
    }

    @Nonnull
    @Override
    public String getName() {
        return RsBundle.message("settings.rust.inlay.hints.title.method.chains");
    }

    @Nullable
    @Override
    public String getPreviewText() {
        return null;
    }

    @Nonnull
    @Override
    public InlayGroup getGroup() {
        return InlayGroup.METHOD_CHAINS_GROUP;
    }

    @Nonnull
    @Override
    public ImmediateConfigurable createConfigurable(@Nonnull Settings settings) {
        return new ImmediateConfigurable() {
            @Nonnull
            @Override
            public String getMainCheckboxText() {
                return RsBundle.message("settings.rust.inlay.hints.for");
            }

            @Nonnull
            @Override
            public List<Case> getCases() {
                // which cannot be properly implemented in Java.
                return Collections.emptyList();
            }

            @Nonnull
            public JComponent createComponent(@Nonnull ChangeListener listener) {
                return new JPanel();
            }
        };
    }

    @Nonnull
    @Override
    public Settings createSettings() {
        return new Settings();
    }

    @Nonnull
    @Override
    public InlayHintsCollector getCollectorFor(@Nonnull PsiFile file, @Nonnull Editor editor, @Nonnull Settings settings, @Nonnull InlayHintsSink sink) {
        Project project = file.getProject();
        Crate crate = file instanceof RsFile ? ((RsFile) file).getCrate() : null;

        return new FactoryInlayHintsCollector(editor) {
            private final RsTypeHintsPresentationFactory typeHintsFactory = new RsTypeHintsPresentationFactory(getFactory(), true);
            private final ThreadLocal<WeakReference<Object[]>> lookupAndIteratorTrait = new ThreadLocal<>();

            @Override
            public boolean collect(@Nonnull PsiElement element, @Nonnull Editor editor, @Nonnull InlayHintsSink sink) {
                if (DumbService.isDumb(project)) return true;
                if (!(element instanceof RsMethodCall)) return true;
                RsMethodCall methodCall = (RsMethodCall) element;
                if (!isLastInChain(methodCall)) return true;
                RsCodeStatus codeStatus = RsElementUtil.getCodeStatus(methodCall, crate);
                if (codeStatus == RsCodeStatus.CFG_DISABLED) return true;
                boolean isAttrProcMacro = codeStatus == RsCodeStatus.ATTR_PROC_MACRO_CALL;

                Object[] lookupAndIter = getLookupAndIterator(file);
                ImplLookup lookup = (ImplLookup) lookupAndIter[0];
                @SuppressWarnings("unchecked")
                BoundElement<RsTraitItem> iterator = (BoundElement<RsTraitItem>) lookupAndIter[1];

                List<RsMethodCall> chain = collectChain(methodCall);
                List<RsMethodCall> chainExpanded;
                if (isAttrProcMacro) {
                    chainExpanded = collectExpandedChain(methodCall);
                    if (chainExpanded == null || chainExpanded.size() != chain.size()) return true;
                } else {
                    chainExpanded = chain;
                }

                Ty lastType = null;
                int size = Math.min(chain.size(), chainExpanded.size());
                for (int i = 0; i < size - 1; i++) {
                    RsMethodCall call = chain.get(i);
                    RsMethodCall callExpanded = chainExpanded.get(i);
                    if (!isLastOnLine(call)) continue;
                    Ty type = normalizeType(getMethodCallType(callExpanded), lookup, iterator, settings);
                    if (type != TyUnknown.INSTANCE) {
                        if (settings.myShowSameConsecutiveTypes || !type.isEquivalentTo(lastType)) {
                            InlayPresentation presentation = typeHintsFactory.typeHint(type);
                            InsetPresentation finalPresentation = withDisableAction(presentation, project);
                            sink.addInlineElement(call.getTextRange().getEndOffset(), true, finalPresentation, false);
                        }
                        lastType = type;
                    }
                }

                return true;
            }
        };
    }

    @Nonnull
    private Object[] getLookupAndIterator(@Nonnull PsiFile file) {
        if (file instanceof RsFile) {
            ImplLookup lookup = RsTypesUtil.getImplLookup((RsFile) file);
            KnownItems items = RsTypesUtil.getKnownItems((RsFile) file);
            RsTraitItem iteratorTrait = items.getIterator();
            BoundElement<RsTraitItem> iterator = iteratorTrait != null
                ? RsGenericDeclarationUtil.withDefaultSubst(iteratorTrait)
                : null;
            return new Object[]{lookup, iterator};
        }
        return new Object[]{null, null};
    }

    @Nonnull
    private static InsetPresentation withDisableAction(@Nonnull InlayPresentation presentation, @Nonnull Project project) {
        return new InsetPresentation(
            new MenuOnClickPresentation(presentation, project, () ->
                Collections.singletonList(new InlayProviderDisablingAction(
                    RsBundle.message("settings.rust.inlay.hints.title.method.chains"),
                    RsLanguage.INSTANCE, project, KEY))
            ), 1, 0, 0, 0
        );
    }

    private static Ty normalizeType(@Nonnull Ty type, @Nullable ImplLookup lookup, @Nullable BoundElement<RsTraitItem> iteratorTrait, @Nonnull Settings settings) {
        if (!settings.myIteratorSpecialCase || iteratorTrait == null) return type;
        if (lookup == null) return type;
        Map<RsTypeAlias, Ty> assoc = lookup.selectAllProjectionsStrict(new TraitRef(type, iteratorTrait));
        if (assoc == null) return type;
        return new TyAnon(null, Collections.singletonList(new BoundElement<>(iteratorTrait.getTypedElement(), iteratorTrait.getSubst(), assoc)));
    }

    private static boolean isLastInChain(@Nonnull RsMethodCall methodCall) {
        RsDotExpr parentDotExpr = RsMethodCallUtil.getParentDotExpr(methodCall);
        if (parentDotExpr.getParent() instanceof RsDotExpr) return false;
        return RsPsiJavaUtil.childOfType(parentDotExpr.getExpr(), RsMethodCall.class) != null;
    }

    private static boolean isLastOnLine(@Nonnull RsMethodCall methodCall) {
        return isElementLastOnLine(RsMethodCallUtil.getParentDotExpr(methodCall));
    }

    private static boolean isElementLastOnLine(@Nonnull PsiElement element) {
        PsiElement sibling = element.getNextSibling();
        if (sibling instanceof PsiWhiteSpace) {
            return sibling.textContains('\n') || isElementLastOnLine(sibling);
        }
        if (sibling instanceof PsiComment) {
            return isElementLastOnLine(sibling);
        }
        return false;
    }

    @Nonnull
    private static Ty getMethodCallType(@Nonnull RsMethodCall methodCall) {
        return RsTypesUtil.getType(RsMethodCallUtil.getParentDotExpr(methodCall));
    }

    @Nonnull
    private static List<RsMethodCall> collectChain(@Nonnull RsMethodCall call) {
        List<RsMethodCall> chain = new ArrayList<>();
        RsMethodCall current = call;
        while (true) {
            chain.add(current);
            RsMethodCall next = RsPsiJavaUtil.childOfType(RsMethodCallUtil.getParentDotExpr(current).getExpr(), RsMethodCall.class);
            if (next == null) break;
            current = next;
        }
        Collections.reverse(chain);
        return chain;
    }

    @Nullable
    private static List<RsMethodCall> collectExpandedChain(@Nonnull RsMethodCall call) {
        PsiElement leaf = call.getIdentifier();
        List<PsiElement> expanded = MacroExpansionExtUtil.findExpansionElements(leaf);
        if (expanded == null || expanded.size() != 1) return null;
        PsiElement leafExpanded = expanded.get(0);
        if (!(leafExpanded.getParent() instanceof RsMethodCall)) return null;
        RsMethodCall callExpanded = (RsMethodCall) leafExpanded.getParent();
        return collectChain(callExpanded);
    }

    private static boolean isNewSettingsEnabled() {
        return Registry.is("new.inlay.settings", false);
    }

    public static class Settings {
        private boolean myShowSameConsecutiveTypes = true;
        private boolean myIteratorSpecialCase = true;

        public boolean getShowSameConsecutiveTypes() {
            return myShowSameConsecutiveTypes;
        }

        public void setShowSameConsecutiveTypes(boolean value) {
            myShowSameConsecutiveTypes = value;
        }

        public boolean getIteratorSpecialCase() {
            return myIteratorSpecialCase;
        }

        public void setIteratorSpecialCase(boolean value) {
            myIteratorSpecialCase = value;
        }
    }
}
