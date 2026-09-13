/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import consulo.ide.impl.idea.ide.util.treeView.smartTree.SmartTreeStructure;
import consulo.disposer.Disposable;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import consulo.ui.ex.awt.SimpleToolWindowPanel;
import consulo.language.psi.PsiFileFactory;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.tree.AsyncTreeModel;
import consulo.ui.ex.awt.tree.StructureTreeModel;
import consulo.ide.impl.idea.util.ui.Tree;
import jakarta.annotation.Nonnull;
import org.rust.ide.structure.RsStructureViewModel;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.impl.RsReplCodeFragment;
import org.rust.openapiext.VirtualFileExtUtil;

public class RsConsoleVariablesView extends SimpleToolWindowPanel implements Disposable {

    private static final String EMPTY_TEXT = "No variables yet";

    @Nonnull
    private final RsConsoleCodeFragmentContext codeFragmentContext;
    @Nonnull
    private final RsReplCodeFragment variablesFile;
    @Nonnull
    private final SmartTreeStructure treeStructure;
    @Nonnull
    private final StructureTreeModel<SmartTreeStructure> structureTreeModel;

    public RsConsoleVariablesView(@Nonnull Project project, @Nonnull RsConsoleCodeFragmentContext codeFragmentContext) {
        super(true, true);
        this.codeFragmentContext = codeFragmentContext;

        String allCommands = codeFragmentContext.getAllCommandsText();
        variablesFile = (RsReplCodeFragment) PsiFileFactory.getInstance(project)
            .createFileFromText(RsConsoleView.VIRTUAL_FILE_NAME, RsLanguage.INSTANCE, allCommands);
        RsStructureViewModel structureViewModel = new RsStructureViewModel(null, variablesFile);
        treeStructure = new SmartTreeStructure(project, structureViewModel);

        structureTreeModel = new StructureTreeModel<>(treeStructure, this);
        AsyncTreeModel asyncTreeModel = new AsyncTreeModel(structureTreeModel, this);

        Tree tree = new Tree(asyncTreeModel);
        tree.setRootVisible(false);
        tree.getEmptyText().setText(EMPTY_TEXT);

        setContent(ScrollPaneFactory.createScrollPane(tree));
    }

    public void rebuild() {
        ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                String allCommands = codeFragmentContext.getAllCommandsText();
                var document = VirtualFileExtUtil.getDocument(variablesFile.getVirtualFile());
                if (document != null) {
                    document.setText(allCommands);
                }
            });

            structureTreeModel.getInvoker().invokeLater(() -> {
                treeStructure.rebuildTree();
                structureTreeModel.invalidate(null, true); // invalidateAsync isn't in Consulo
            });
        });
    }

    @Override
    public void dispose() {
    }
}
