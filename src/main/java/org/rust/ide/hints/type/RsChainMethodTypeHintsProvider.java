/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.type;

import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.ImmediateConfigurable.Case;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.InsetPresentation;
import com.intellij.codeInsight.hints.presentation.MenuOnClickPresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @NotNull
    @Override
    public SettingsKey<Settings> getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public String getName() {
        return RsBundle.message("settings.rust.inlay.hints.title.method.chains");
    }

    @Nullable
    @Override
    public String getPreviewText() {
        return null;
    }

    @NotNull
    @Override
    public InlayGroup getGroup() {
        return InlayGroup.METHOD_CHAINS_GROUP;
    }

    @NotNull
    @Override
    public ImmediateConfigurable createConfigurable(@NotNull Settings settings) {
        return new ImmediateConfigurable() {
            @NotNull
            @Override
            public String getMainCheckboxText() {
                return RsBundle.message("settings.rust.inlay.hints.for");
            }

            @NotNull
            @Override
            public List<Case> getCases() {
                // which cannot be properly implemented in Java.
                return Collections.emptyList();
            }

            @NotNull
            @Override
            public JComponent createComponent(@NotNull ChangeListener listener) {
                return new JPanel();
            }
        };
    }

    @NotNull
    @Override
    public Settings createSettings() {
        return new Settings();
    }

    @NotNull
    @Override
    public InlayHintsCollector getCollectorFor(@NotNull PsiFile file, @NotNull Editor editor, @NotNull Settings settings, @NotNull InlayHintsSink sink) {
        Project project = file.getProject();
        Crate crate = file instanceof RsFile ? ((RsFile) file).getCrate() : null;

        return new FactoryInlayHintsCollector(editor) {
            private final RsTypeHintsPresentationFactory typeHintsFactory = new RsTypeHintsPresentationFactory(getFactory(), true);
            private final ThreadLocal<WeakReference<Object[]>> lookupAndIteratorTrait = new ThreadLocal<>();

            @Override
            public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
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

    @NotNull
    private Object[] getLookupAndIterator(@NotNull PsiFile file) {
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

    @NotNull
    private static InsetPresentation withDisableAction(@NotNull InlayPresentation presentation, @NotNull Project project) {
        return new InsetPresentation(
            new MenuOnClickPresentation(presentation, project, () ->
                Collections.singletonList(new InlayProviderDisablingAction(
                    RsBundle.message("settings.rust.inlay.hints.title.method.chains"),
                    RsLanguage.INSTANCE, project, KEY))
            ), 1, 0, 0, 0
        );
    }

    private static Ty normalizeType(@NotNull Ty type, @Nullable ImplLookup lookup, @Nullable BoundElement<RsTraitItem> iteratorTrait, @NotNull Settings settings) {
        if (!settings.myIteratorSpecialCase || iteratorTrait == null) return type;
        if (lookup == null) return type;
        Map<RsTypeAlias, Ty> assoc = lookup.selectAllProjectionsStrict(new TraitRef(type, iteratorTrait));
        if (assoc == null) return type;
        return new TyAnon(null, Collections.singletonList(new BoundElement<>(iteratorTrait.getTypedElement(), iteratorTrait.getSubst(), assoc)));
    }

    private static boolean isLastInChain(@NotNull RsMethodCall methodCall) {
        RsDotExpr parentDotExpr = RsMethodCallUtil.getParentDotExpr(methodCall);
        if (parentDotExpr.getParent() instanceof RsDotExpr) return false;
        return RsPsiJavaUtil.childOfType(parentDotExpr.getExpr(), RsMethodCall.class) != null;
    }

    private static boolean isLastOnLine(@NotNull RsMethodCall methodCall) {
        return isElementLastOnLine(RsMethodCallUtil.getParentDotExpr(methodCall));
    }

    private static boolean isElementLastOnLine(@NotNull PsiElement element) {
        PsiElement sibling = element.getNextSibling();
        if (sibling instanceof PsiWhiteSpace) {
            return sibling.textContains('\n') || isElementLastOnLine(sibling);
        }
        if (sibling instanceof PsiComment) {
            return isElementLastOnLine(sibling);
        }
        return false;
    }

    @NotNull
    private static Ty getMethodCallType(@NotNull RsMethodCall methodCall) {
        return RsTypesUtil.getType(RsMethodCallUtil.getParentDotExpr(methodCall));
    }

    @NotNull
    private static List<RsMethodCall> collectChain(@NotNull RsMethodCall call) {
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
    private static List<RsMethodCall> collectExpandedChain(@NotNull RsMethodCall call) {
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
