/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.imports;

import com.intellij.codeInsight.daemon.impl.ShowAutoImportPass;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.PriorityAction;
import com.intellij.codeInspection.BatchQuickFix;
import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.codeInspection.HintAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.ide.intentions.util.macros.IntentionInMacroUtil;
import org.rust.ide.settings.RsCodeInsightSettings;
import org.rust.ide.utils.imports.ImportCandidate;
import org.rust.ide.utils.imports.ImportCandidatesCollector;
import org.rust.ide.utils.imports.ImportContext;
import org.rust.ide.utils.imports.ImportUtil;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.infer.ResolvedPath;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.openapiext.Testmark;
import org.rust.openapiext.CommandUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.psi.ext.RsElement;

public class AutoImportFix extends RsQuickFixBase<RsElement> implements BatchQuickFix, PriorityAction, HintAction {

    @IntentionFamilyName
    public static final String NAME = RsBundle.message("intention.name.import");

    private boolean myIsConsumed = false;
    private final Context myContext;

    public AutoImportFix(@NotNull PsiElement element, @NotNull Context context) {
        super((RsElement) element);
        this.myContext = context;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return NAME;
    }

    @NotNull
    @Override
    public String getText() {
        return getFamilyName();
    }

    @Override
    public boolean isAvailable() {
        return super.isAvailable() && !myIsConsumed;
    }

    @NotNull
    @Override
    public PriorityAction.Priority getPriority() {
        return PriorityAction.Priority.TOP;
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsElement element) {
        CommandUtil.checkWriteAccessNotAllowed();
        List<ImportCandidate> candidates = myContext.myCandidates;
        if (candidates.size() == 1) {
            CommandUtil.runWriteCommandAction(project, getText(), () -> {
                ImportUtil.doImport(candidates.get(0), element);
                if (editor != null) {
                    IntentionInMacroUtil.finishActionInMacroExpansionCopy(editor);
                }
            });
        } else {
            DataManager.getInstance().getDataContextFromFocusAsync().onSuccess(dataContext -> {
                chooseItemAndImport(project, editor, dataContext, candidates, element);
            });
        }
        myIsConsumed = true;
    }

    @Override
    public void applyFix(
        @NotNull Project project,
        @NotNull CommonProblemDescriptor[] descriptors,
        @NotNull List<PsiElement> psiElementsToIgnore,
        @Nullable Runnable refreshViews
    ) {
        CommandUtil.runWriteCommandAction(project, getText(), () -> {
            for (CommonProblemDescriptor descriptor : descriptors) {
                com.intellij.codeInspection.QuickFix<?>[] fixes = descriptor.getFixes();
                if (fixes == null) continue;
                AutoImportFix fix = null;
                for (com.intellij.codeInspection.QuickFix<?> f : fixes) {
                    if (f instanceof AutoImportFix) {
                        fix = (AutoImportFix) f;
                        break;
                    }
                }
                if (fix == null) continue;
                List<ImportCandidate> candidates = fix.myContext.myCandidates;
                if (candidates.size() != 1) continue;
                ImportCandidate candidate = candidates.get(0);
                @SuppressWarnings("deprecation")
                PsiElement startElement = fix.getStartElement();
                if (!(startElement instanceof RsElement)) continue;
                RsElement context = (RsElement) startElement;
                if (RsExpandedElementUtil.isExpandedFromMacro(context)) continue;
                ImportUtil.doImport(candidate, context);
            }
        });
        if (refreshViews != null) refreshViews.run();
    }

    private void chooseItemAndImport(
        @NotNull Project project,
        @Nullable Editor editor,
        @NotNull DataContext dataContext,
        @NotNull List<ImportCandidate> items,
        @NotNull RsElement context
    ) {
        ImportUi.showItemsToImportChooser(project, dataContext, items, selectedValue -> {
            CommandUtil.runWriteCommandAction(project, getText(), () -> {
                ImportUtil.doImport(selectedValue, context);
                if (editor != null) {
                    IntentionInMacroUtil.finishActionInMacroExpansionCopy(editor);
                }
            });
        });
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @NotNull
    @Override
    public PsiFile getElementToMakeWritable(@NotNull PsiFile currentFile) {
        return currentFile;
    }

    @Override
    public boolean showHint(@NotNull Editor editor) {
        if (!RsCodeInsightSettings.getInstance().showImportPopup) return false;
        if (HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(true)) return false;

        @SuppressWarnings("deprecation")
        PsiElement startElem = getStartElement();
        if (!(startElem instanceof RsElement)) return false;
        RsElement element = (RsElement) startElem;
        if (RsExpandedElementUtil.isExpandedFromMacro(element)) return false;
        List<ImportCandidate> candidates = myContext.myCandidates;
        String hint = candidates.get(0).getInfo().getUsePath();
        boolean multiple = candidates.size() > 1;
        String message = ShowAutoImportPass.getMessage(multiple, hint);
        HintManager.getInstance().showQuestionHint(editor, message, element.getTextOffset(), RsElementUtil.getEndOffset(element), () -> {
            invoke(element.getProject(), editor, element);
            return true;
        });
        return true;
    }

    @Nullable
    public static Context findApplicableContext(@NotNull RsPath path) {
        return findApplicableContext(path, ImportContext.Type.AUTO_IMPORT);
    }

    @Nullable
    public static Context findApplicableContext(@NotNull RsPath path, @NotNull ImportContext.Type type) {
        if (path.getReference() == null) return null;

        // `impl Future<Output=i32>`
        //              ~~~~~~ path
        PsiElement parent = path.getParent();
        if (parent instanceof RsAssocTypeBinding) {
            RsAssocTypeBinding binding = (RsAssocTypeBinding) parent;
            if (binding.getEq() != null && binding.getPath() == path) return null;
        }

        RsPath basePath = RsPathUtil.basePath(path);
        if (RsPathUtil.getResolveStatus(basePath) != PathResolveStatus.UNRESOLVED && type != ImportContext.Type.OTHER) return null;

        if (RsElementUtil.ancestorStrict(path, RsUseSpeck.class) != null) {
            // Don't try to import path in use item
            Testmarks.PathInUseItem.hit();
            return null;
        }

        String referenceName = basePath.getReferenceName();
        if (referenceName == null) return null;
        ImportContext importContext = ImportContext.from(path, type);
        if (importContext == null) return null;
        List<ImportCandidate> candidates = ImportCandidatesCollector.getImportCandidates(importContext, referenceName);

        return new Context(Type.GENERAL_PATH, candidates);
    }

    @Nullable
    public static Context findApplicableContext(@NotNull RsPatBinding pat) {
        ImportContext importContext = ImportContext.from(pat);
        if (importContext == null) return null;
        List<ImportCandidate> candidates = ImportCandidatesCollector.getImportCandidates(importContext, pat.getReferenceName());
        if (candidates.isEmpty()) return null;
        return new Context(Type.GENERAL_PATH, candidates);
    }

    @Nullable
    public static Context findApplicableContext(@NotNull RsMethodCall methodCall) {
        List<org.rust.lang.core.resolve.ref.MethodResolveVariant> results = ExtensionsUtil.getInference(methodCall) != null
            ? ExtensionsUtil.getInference(methodCall).getResolvedMethod(methodCall)
            : Collections.emptyList();
        if (results == null) results = Collections.emptyList();
        if (results.isEmpty()) return new Context(Type.METHOD, Collections.emptyList());
        List<ImportCandidate> candidates = ImportCandidatesCollector.getImportCandidates(methodCall, results);
        if (candidates == null) return null;
        return new Context(Type.METHOD, candidates);
    }

    /** Import traits for type-related UFCS method calls and assoc items */
    @Nullable
    public static Context findApplicableContextForAssocItemPath(@NotNull RsPath path) {
        PsiElement parent = path.getParent();
        if (!(parent instanceof RsPathExpr)) return null;

        // `std::default::Default::default()`
        RsPath qualifier = RsPathUtil.getQualifier(path);
        if (qualifier != null) {
            PsiElement qualifierElement = qualifier.getReference() != null ? qualifier.getReference().resolve() : null;
            if (qualifierElement instanceof RsTraitItem) return null;
        }

        // `<Foo as bar::Baz>::qux()`
        RsTypeQual typeQual = path.getTypeQual();
        if (typeQual != null && typeQual.getTraitRef() != null) return null;

        RsPathExpr pathExpr = (RsPathExpr) parent;
        List<ResolvedPath> resolved = ExtensionsUtil.getInference(path) != null
            ? ExtensionsUtil.getInference(path).getResolvedPath(pathExpr)
            : null;
        if (resolved == null) return null;
        List<Object> sources = new ArrayList<>();
        for (ResolvedPath rp : resolved) {
            if (!(rp instanceof ResolvedPath.AssocItem)) return null;
            sources.add(((ResolvedPath.AssocItem) rp).getSource());
        }
        List<ImportCandidate> candidates = ImportCandidatesCollector.getTraitImportCandidates(path, sources);
        if (candidates == null) return null;
        return new Context(Type.ASSOC_ITEM_PATH, candidates);
    }

    public static class Context {
        private final Type myType;
        private final List<ImportCandidate> myCandidates;

        public Context(@NotNull Type type, @NotNull List<ImportCandidate> candidates) {
            this.myType = type;
            this.myCandidates = candidates;
        }

        @NotNull
        public Type getType() {
            return myType;
        }

        @NotNull
        public List<ImportCandidate> getCandidates() {
            return myCandidates;
        }
    }

    public enum Type {
        GENERAL_PATH,
        ASSOC_ITEM_PATH,
        METHOD
    }

    public static class Testmarks {
        public static final Testmark PathInUseItem = new Testmark() {};
    }
}
