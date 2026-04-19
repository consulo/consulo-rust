/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import com.intellij.ide.util.treeView.smartTree.SmartTreeStructure;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.psi.PsiFileFactory;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.structure.RsStructureViewModel;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsReplCodeFragment;
import org.rust.openapiext.VirtualFileExtUtil;

public class RsConsoleVariablesView extends SimpleToolWindowPanel implements Disposable {

    private static final String EMPTY_TEXT = "No variables yet";

    @NotNull
    private final RsConsoleCodeFragmentContext codeFragmentContext;
    @NotNull
    private final RsReplCodeFragment variablesFile;
    @NotNull
    private final SmartTreeStructure treeStructure;
    @NotNull
    private final StructureTreeModel<SmartTreeStructure> structureTreeModel;

    public RsConsoleVariablesView(@NotNull Project project, @NotNull RsConsoleCodeFragmentContext codeFragmentContext) {
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
                structureTreeModel.invalidateAsync();
            });
        });
    }

    @Override
    public void dispose() {
    }
}
