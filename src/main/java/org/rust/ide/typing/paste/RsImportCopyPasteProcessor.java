/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing.paste;

import com.intellij.codeInsight.daemon.impl.CollectHighlightsUtil;
import com.intellij.codeInsight.editorActions.CopyPastePostProcessor;
import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.openapi.util.Pair;
import org.rust.ide.fixes.QualifyPathFix;
import org.rust.ide.inspections.imports.AutoImportFix;
import org.rust.ide.settings.RsCodeInsightSettings;
import org.rust.ide.utils.imports.ImportCandidate;
import org.rust.ide.utils.imports.ImportCandidateUtil;
import org.rust.ide.utils.imports.ImportContext;
import org.rust.ide.utils.imports.ImportInfo;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.openapiext.PsiFileExtUtil;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.*;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

public class RsImportCopyPasteProcessor extends CopyPastePostProcessor<RsTextBlockTransferableData> {

    public static final DataFlavor dataFlavor;

    static {
        DataFlavor flavor;
        try {
            Class<?> dataClass = RsReferenceData.class;
            flavor = new DataFlavor(
                DataFlavor.javaJVMLocalObjectMimeType + ";class=" + dataClass.getName(),
                "RsReferenceData",
                dataClass.getClassLoader()
            );
        } catch (NoClassDefFoundError | IllegalArgumentException | ClassNotFoundException e) {
            flavor = null;
        }
        dataFlavor = flavor;
    }

    @Override
    public List<RsTextBlockTransferableData> collectTransferableData(
        PsiFile file, Editor editor, int[] startOffsets, int[] endOffsets
    ) {
        if (!(file instanceof RsFile) || DumbService.getInstance(file.getProject()).isDumb()) {
            return Collections.emptyList();
        }
        if (!RsCodeInsightSettings.getInstance().importOnPaste) return Collections.emptyList();

        if (startOffsets.length != 1 || endOffsets.length != 1) return Collections.emptyList();
        int startOffset = startOffsets[0];
        int endOffset = endOffsets[0];
        TextRange range = new TextRange(startOffset, endOffset);

        if (range.equals(file.getTextRange())) return Collections.emptyList();

        ImportMap map = createFqnMap((RsFile) file, range);
        return Collections.singletonList(new RsTextBlockTransferableData(map));
    }

    @Override
    public List<RsTextBlockTransferableData> extractTransferableData(Transferable content) {
        try {
            Object data = content.getTransferData(dataFlavor);
            if (data instanceof RsTextBlockTransferableData) {
                return Collections.singletonList((RsTextBlockTransferableData) data);
            }
            return Collections.emptyList();
        } catch (Throwable e) {
            return Collections.emptyList();
        }
    }

    @Override
    public void processTransferableData(
        Project project, Editor editor, RangeMarker bounds, int caretOffset,
        Ref<? super Boolean> indented, List<? extends RsTextBlockTransferableData> values
    ) {
        if (!RsCodeInsightSettings.getInstance().importOnPaste) return;

        PsiDocumentManager.getInstance(project).commitAllDocuments();

        RsTextBlockTransferableData data = values.isEmpty() ? null : values.get(0);
        if (data == null) return;
        PsiFile psiFile = PsiFileExtUtil.toPsiFile(editor.getDocument(), project);
        if (!(psiFile instanceof RsFile)) return;
        RsFile file = (RsFile) psiFile;

        PsiElement commonParent = CollectHighlightsUtil.findCommonParent(file, bounds.getStartOffset(), bounds.getEndOffset());
        RsElement rsElement = commonParent instanceof RsElement ? (RsElement) commonParent : null;
        if (rsElement == null) return;
        RsMod containingMod = RsElementUtil.getContainingModOrSelf(rsElement);
        if (containingMod == null) return;
        PsiElement importCtxPsi = RsModUtil.getFirstItem(containingMod);
        if (importCtxPsi == null) return;
        if (!(importCtxPsi instanceof RsElement)) return;
        RsElement importCtx = (RsElement) importCtxPsi;

        int importOffset = bounds.getTextRange().getStartOffset();

        ImportingProcessor processor = new ImportingProcessor(importOffset, data.getImportMap());

        ApplicationManager.getApplication().runWriteAction(() -> {
            processElementsInRange(file, bounds.getTextRange(), processor);
            for (ImportCandidate candidate : processor.getImportCandidates()) {
                ImportCandidateUtil.doImport(candidate, importCtx);
            }
            for (Pair<RsPath, ImportInfo> pair : processor.getQualifyCandidates()) {
                QualifyPathFix.qualify(pair.getFirst(), pair.getSecond());
            }
        });
    }

    private static ImportMap createFqnMap(RsFile file, TextRange range) {
        Map<Integer, QualifiedItemPath> fqnMap = new HashMap<>();

        ElementProcessor processor = new ElementProcessor() {
            @Override
            public void processPath(RsPath path) {
                if (RsPathUtil.getQualifier(path) != null) return;
                PsiElement resolved = path.getReference() != null ? path.getReference().resolve() : null;
                if (resolved instanceof RsQualifiedNamedElement) {
                    storeMapping(path, (RsQualifiedNamedElement) resolved);
                }
            }

            @Override
            public void processMethodCall(RsMethodCall methodCall) {
                // Method call import mapping
            }

            @Override
            public void processPatBinding(RsPatBinding binding) {
                PsiElement target = binding.getReference().resolve();
                if (target instanceof RsQualifiedNamedElement) {
                    storeMapping(binding, (RsQualifiedNamedElement) target);
                }
            }

            private void storeMapping(RsElement element, RsQualifiedNamedElement target) {
                String crateRelativePath = target.getCrateRelativePath();
                if (crateRelativePath == null) return;
                Crate crate = RsElementUtil.getContainingCrate(target);
                Integer crateId = crate != null ? crate.getId() : null;
                if (crateId == null) return;
                fqnMap.put(toRelativeEndOffset(element, range.getStartOffset()),
                    new QualifiedItemPath(crateRelativePath, crateId));
            }
        };

        processElementsInRange(file, range, processor);
        return new ImportMap(fqnMap);
    }

    private static void processElementsInRange(RsFile file, TextRange range, ElementProcessor processor) {
        List<PsiElement> elements = CollectHighlightsUtil.getElementsInRange(file, range.getStartOffset(), range.getEndOffset());
        for (PsiElement element : elements) {
            if (element instanceof RsPath) {
                processor.processPath((RsPath) element);
            } else if (element instanceof RsMethodCall) {
                processor.processMethodCall((RsMethodCall) element);
            } else if (element instanceof RsPatBinding) {
                processor.processPatBinding((RsPatBinding) element);
            }
        }
    }

    private static int toRelativeEndOffset(PsiElement element, int importOffset) {
        return element.getTextRange().getEndOffset() - importOffset;
    }

    private interface ElementProcessor {
        void processPath(RsPath path);
        void processMethodCall(RsMethodCall methodCall);
        void processPatBinding(RsPatBinding binding);
    }

    private static class ImportingProcessor implements ElementProcessor {
        private final int myImportOffset;
        private final ImportMap myImportMap;
        private final List<ImportCandidate> myImportCandidates = new ArrayList<>();
        private final List<Pair<RsPath, ImportInfo>> myQualifyCandidates = new ArrayList<>();

        ImportingProcessor(int importOffset, ImportMap importMap) {
            myImportOffset = importOffset;
            myImportMap = importMap;
        }

        List<ImportCandidate> getImportCandidates() {
            return myImportCandidates;
        }

        List<Pair<RsPath, ImportInfo>> getQualifyCandidates() {
            return myQualifyCandidates;
        }

        @Override
        public void processPath(RsPath path) {
            handleImport(path, () -> AutoImportFix.findApplicableContext(path));
        }

        @Override
        public void processMethodCall(RsMethodCall methodCall) {
            handleImport(methodCall, () -> AutoImportFix.findApplicableContext(methodCall));
        }

        @Override
        public void processPatBinding(RsPatBinding binding) {
            handleImport(binding, () -> AutoImportFix.findApplicableContext(binding));
        }

        private void handleImport(RsElement element, java.util.function.Supplier<AutoImportFix.Context> getCtx) {
            QualifiedItemPath importMapCandidate = myImportMap.elementToFqn(element, myImportOffset);
            if (importMapCandidate == null) return;
            AutoImportFix.Context ctx = getCtx.get();

            ImportCandidate candidate = getCandidate(ctx, importMapCandidate);
            if (candidate != null) {
                myImportCandidates.add(candidate);
                return;
            }

            if (element instanceof RsPath) {
                RsPath path = (RsPath) element;
                Collection<? extends PsiElement> resolvedTargets = path.getReference() != null
                    ? path.getReference().multiResolve()
                    : Collections.emptyList();
                if (resolvedTargets.isEmpty()) {
                    Crate elementCrate = RsElementUtil.getContainingCrate(element);
                    if (elementCrate != null && java.util.Objects.equals(importMapCandidate.getCrateId(), elementCrate.getId())) {
                        ImportInfo importInfo = new ImportInfo.LocalImportInfo(
                            "crate" + importMapCandidate.getCrateRelativePath()
                        );
                        myQualifyCandidates.add(new Pair<>(path, importInfo));
                    }
                } else {
                    if (resolvedTargets.size() == 1) {
                        PsiElement resolvedTarget = resolvedTargets.iterator().next();
                        if (resolvedTarget instanceof RsQualifiedNamedElement
                            && importMapCandidate.matches((RsQualifiedNamedElement) resolvedTarget)) {
                            return;
                        }
                    }
                    AutoImportFix.Context otherCtx = AutoImportFix.findApplicableContext(path, ImportContext.Type.OTHER);
                    ImportCandidate otherCandidate = getCandidate(otherCtx, importMapCandidate);
                    if (otherCandidate != null) {
                        myQualifyCandidates.add(new Pair<>(path, otherCandidate.getInfo()));
                    }
                }
            }
        }
    }

    private static ImportCandidate getCandidate(AutoImportFix.Context ctx, QualifiedItemPath originalItem) {
        if (ctx == null) return null;
        for (ImportCandidate c : ctx.getCandidates()) {
            if (originalItem.matches(c.getItem())) return c;
        }
        return null;
    }

    private static class RsReferenceData {
    }
}
