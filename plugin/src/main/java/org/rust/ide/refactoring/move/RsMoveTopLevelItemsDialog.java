/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move;

import consulo.undoRedo.CommandProcessor;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.util.dataholder.Key;

import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.ui.RefactoringDialog;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.JBTextField;
import com.intellij.ui.dsl.builder.AlignY;
import consulo.language.util.IncorrectOperationException;
import consulo.ui.ex.awt.JBUI;
import org.apache.commons.lang3.StringEscapeUtils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.docs.RsDocumentationProvider;
import org.rust.lang.RsConstants;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsItemElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.impl.RsModUtil;
import org.rust.openapiext.*;
import org.rust.stdext.StdextUtil;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import consulo.language.psi.PsiFile;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.awt.util.StringHtmlUtil;
import consulo.ui.image.Image;
import consulo.util.io.FileUtil;
import org.rust.openapiext.OpenApiUtil;
import org.rust.lang.core.psi.impl.*;

public class RsMoveTopLevelItemsDialog extends RefactoringDialog {

    private static final Logger LOG = Logger.getInstance(RsMoveTopLevelItemsDialog.class);

    @Nonnull
    private final Set<RsItemElement> itemsToMove;
    @Nonnull
    private final RsMod sourceMod;
    
    @Nonnull
    private final String sourceFilePath;
    @Nonnull
    private final JBTextField sourceFileField;
    @Nonnull
    private final TextFieldWithBrowseButton targetFileChooser;
    @Nonnull
    private final RsMoveMemberSelection.RsMoveMemberSelectionPanel memberPanel;

    private boolean searchForReferences = true;

    public RsMoveTopLevelItemsDialog(
        @Nonnull Project project,
        @Nonnull Set<RsItemElement> itemsToMove,
        @Nonnull RsMod sourceMod
    ) {
        super(project, false);
        this.itemsToMove = itemsToMove;
        this.sourceMod = sourceMod;
        this.sourceFilePath = sourceMod.getContainingFile().getVirtualFile().getPath();
        this.sourceFileField = new JBTextField(sourceFilePath);
        this.sourceFileField.setEnabled(false);
        this.targetFileChooser = createTargetFileChooser(project);
        this.memberPanel = createMemberSelectionPanel();
        this.memberPanel.setPreferredSize(JBUI.size(0, 0));

        if (org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
            throw new IllegalStateException("Should not be used in unit test mode");
        }
        super.init();
        setTitle(RsBundle.message("dialog.title.move.module.items"));
        validateButtons();
    }

    @Nonnull
    private TextFieldWithBrowseButton createTargetFileChooser(@Nonnull Project project) {
        TextFieldWithBrowseButton chooser = org.rust.openapiext.ui.UiUtil.pathToRsFileTextField(
            getDisposable(), RsBundle.message("dialog.title.choose.destination.file"), project, this::validateButtons
        );
        chooser.setText(sourceFilePath);
        chooser.getTextField().setCaretPosition(sourceFilePath.length() - 3); // before ".rs"
        chooser.getTextField().moveCaretPosition(sourceFilePath.lastIndexOf('/') + 1);
        return chooser;
    }

    @Nonnull
    private RsMoveMemberSelection.RsMoveMemberSelectionPanel createMemberSelectionPanel() {
        List<RsItemElement> topLevelItems = getTopLevelItems();
        Map<RsItemElement, List<RsImplItem>> grouped = RsMoveTopLevelItemsHandler.groupImplsByStructOrTrait(sourceMod, new HashSet<>(topLevelItems));
        List<RsMoveMemberSelection.RsMoveNodeInfo> nodesGroupedWithImpls = new ArrayList<>();
        Set<RsItemElement> itemsGroupedWithImpls = new HashSet<>();

        for (Map.Entry<RsItemElement, List<RsImplItem>> entry : grouped.entrySet()) {
            RsMoveItemAndImplsInfo info = new RsMoveItemAndImplsInfo(entry.getKey(), entry.getValue());
            nodesGroupedWithImpls.add(info);
            for (RsMoveMemberSelection.RsMoveNodeInfo child : info.getChildren()) {
                itemsGroupedWithImpls.add(((RsMoveMemberInfo) child).getMember());
            }
        }

        List<RsMoveMemberSelection.RsMoveNodeInfo> nodesWithoutGrouping = topLevelItems.stream()
            .filter(item -> !itemsGroupedWithImpls.contains(item))
            .map(RsMoveMemberInfo::new)
            .collect(Collectors.toList());

        List<RsMoveMemberSelection.RsMoveNodeInfo> nodesAll = new ArrayList<>(nodesGroupedWithImpls);
        nodesAll.addAll(nodesWithoutGrouping);

        List<RsMoveMemberSelection.RsMoveNodeInfo> nodesSelected = nodesAll.stream()
            .flatMap(it -> {
                if (it instanceof RsMoveItemAndImplsInfo) {
                    return ((RsMoveItemAndImplsInfo) it).getChildren().stream();
                } else if (it instanceof RsMoveMemberInfo) {
                    return java.util.stream.Stream.of(it);
                } else {
                    throw new IllegalStateException("unexpected node info type: " + it);
                }
            })
            .filter(it -> itemsToMove.contains(((RsMoveMemberInfo) it).getMember()))
            .collect(Collectors.toList());

        RsMoveMemberSelection.RsMoveMemberSelectionPanel panel = new RsMoveMemberSelection.RsMoveMemberSelectionPanel(
            myProject, RsBundle.message("separator.items.to.move"), nodesAll, nodesSelected
        );
        panel.getTree().setInclusionListener((Runnable) () -> validateButtons());
        return panel;
    }

    @Override
    protected @Nonnull JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        JPanel fromRow = new JPanel(new BorderLayout(8, 0));
        fromRow.add(new JLabel(RsBundle.message("from")), BorderLayout.WEST);
        fromRow.add(sourceFileField, BorderLayout.CENTER);
        JPanel toRow = new JPanel(new BorderLayout(8, 0));
        toRow.add(new JLabel(RsBundle.message("to")), BorderLayout.WEST);
        toRow.add(targetFileChooser, BorderLayout.CENTER);
        topPanel.add(fromRow);
        topPanel.add(toRow);
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(memberPanel, BorderLayout.CENTER);

        JCheckBox searchCheckBox = new JCheckBox(RefactoringBundle.message("search.for.references"), searchForReferences);
        searchCheckBox.addActionListener(e -> searchForReferences = searchCheckBox.isSelected());
        panel.add(searchCheckBox, BorderLayout.SOUTH);

        panel.setPreferredSize(new Dimension(600, 400));
        return panel;
    }

    @Nonnull
    private List<RsItemElement> getTopLevelItems() {
        List<RsItemElement> result = new ArrayList<>();
        for (consulo.language.psi.PsiElement child : sourceMod.getChildren()) {
            if (child instanceof RsItemElement && RsMoveTopLevelItemsHandler.canMoveElement(child)) {
                result.add((RsItemElement) child);
            }
        }
        return result;
    }

    @Override
    protected boolean areButtonsValid() {
        if (memberPanel == null) return false;
        return !sourceFilePath.equals(targetFileChooser.getText()) && !getSelectedItems().isEmpty();
    }

    @Nonnull
    private Set<RsItemElement> getSelectedItems() {
        Set<RsItemElement> result = new LinkedHashSet<>();
        for (Object obj : memberPanel.getTree().getIncludedSet()) {
            if (obj instanceof RsMoveMemberInfo) {
                result.add(((RsMoveMemberInfo) obj).getMember());
            }
        }
        return result;
    }

    @Override
    protected void doAction() {
        CommandProcessor.getInstance().executeCommand(
            myProject,
            this::doActionUndoCommand,
            RefactoringBundle.message("move.title"),
            null
        );
    }

    private void doActionUndoCommand() {
        Set<RsItemElement> items = getSelectedItems();
        Path targetFilePath = java.nio.file.Paths.get(targetFileChooser.getText());
        RsMod targetMod = getOrCreateTargetMod(targetFilePath, myProject, sourceMod.getCrateRoot());
        if (targetMod == null) return;
        try {
            RsMoveTopLevelItemsProcessor processor = new RsMoveTopLevelItemsProcessor(myProject, items, targetMod, searchForReferences);
            invokeRefactoring(processor);
        } catch (Exception e) {
            if (!(e instanceof IncorrectOperationException)) {
                LOG.error(e);
            }
            showErrorMessage(myProject, e.getMessage());
        }
    }

    @Nullable
    public static RsMod getOrCreateTargetMod(@Nonnull Path targetFilePath, @Nonnull Project project, @Nullable RsMod crateRoot) {
        VirtualFile targetFile = LocalFileSystem.getInstance().findFileByNioFile(targetFilePath);
        if (targetFile != null) {
            consulo.language.psi.PsiFile psiFile = VirtualFileExtUtil.toPsiFile(targetFile, project);
            if (psiFile instanceof RsMod) {
                return (RsMod) psiFile;
            }
            showErrorMessage(project, RsBundle.message("dialog.message.target.file.must.be.rust.file"));
            return null;
        } else {
            try {
                RsFile newFile = createNewRustFile(targetFilePath, project, crateRoot);
                if (newFile == null) {
                    showErrorMessage(project, RsBundle.message("dialog.message.can.t.create.new.rust.file.or.attach.it.to.module.tree"));
                    return null;
                }
                return newFile;
            } catch (Exception e) {
                showErrorMessage(project, RsBundle.message("dialog.message.error.during.creating.new.rust.file", e.getMessage() != null ? e.getMessage() : ""));
                return null;
            }
        }
    }

    private static void showErrorMessage(@Nonnull Project project,  @Nullable String message) {
        String title = RefactoringBundle.message("error.title");
        CommonRefactoringUtil.showErrorMessage(title, message, null, project);
    }

    @Nullable
    private static RsFile createNewRustFile(@Nonnull Path filePath, @Nonnull Project project, @Nullable RsMod crateRoot) {
        return org.rust.openapiext.OpenApiUtil.runWriteCommandAction(project, RefactoringBundle.message("move.title"), () -> {
            VirtualFileSystem fileSystem = crateRoot instanceof RsFile && ((RsFile) crateRoot).getVirtualFile() != null
                ? ((RsFile) crateRoot).getVirtualFile().getFileSystem()
                : LocalFileSystem.getInstance();
            return createNewFile(filePath, fileSystem, virtualFile -> {
                consulo.language.psi.PsiFile psiFile = VirtualFileExtUtil.toPsiFile(virtualFile, project);
                if (psiFile == null) return null;
                RsFile rsFile = psiFile instanceof RsFile ? (RsFile) psiFile : null;
                if (rsFile == null) return null;
                if (!attachFileToParentMod(rsFile, project, crateRoot)) return null;
                return rsFile;
            });
        });
    }

    private static boolean attachFileToParentMod(@Nonnull RsFile file, @Nonnull Project project, @Nullable RsMod crateRoot) {
        if (file.isCrateRoot()) return true;
        consulo.language.psi.PsiDirectory parentModOwningDirectory;
        String modName;
        if (RsConstants.MOD_RS_FILE.equals(file.getName())) {
            parentModOwningDirectory = file.getParent() != null ? file.getParent().getParentDirectory() : null;
            modName = file.getParent() != null ? file.getParent().getName() : null;
        } else {
            parentModOwningDirectory = file.getParent();
            modName = consulo.util.io.FileUtil.getNameWithoutExtension(file.getName());
        }
        RsMod parentMod = parentModOwningDirectory != null
            ? RsMoveDirectoryUtils.getOwningMod(parentModOwningDirectory, crateRoot) : null;
        if (parentMod == null || modName == null) return false;
        RsPsiFactory psiFactory = new RsPsiFactory(project);
        RsMoveProcessorUtils.insertModDecl(parentMod, psiFactory, psiFactory.createModDeclItem(modName));
        return true;
    }

    @Nullable
    private static <T> T createNewFile(
        @Nonnull Path filePath,
        @Nonnull VirtualFileSystem fileSystem,
        @Nonnull java.util.function.Function<VirtualFile, T> action
    ) {
        Path directoryPath = filePath.getParent();
        List<Path> directoriesToCreate = new ArrayList<>();
        Path current = directoryPath;
        while (current != null && fileSystem.findFileByPath(current.toString()) == null) {
            directoriesToCreate.add(current);
            current = current.getParent();
        }

        try {
            VirtualFile parentDirectory = VirtualFileUtil.createDirectoryIfMissing(directoryPath.toString());
            if (parentDirectory == null) return null;
            VirtualFile file = parentDirectory.createChildData(null, filePath.getFileName().toString());
            T result = action.apply(file);
            if (result != null) return result;

            // Need to delete created file and directories
            file.delete(null);
            for (Path dir : directoriesToCreate) {
                VirtualFile vf = fileSystem.findFileByPath(dir.toString());
                if (vf != null) vf.delete(null);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static final Key<RsMod> MOVE_TARGET_MOD_KEY = Key.create("RS_MOVE_TARGET_MOD_KEY");
    public static final Key<Path> MOVE_TARGET_FILE_PATH_KEY = Key.create("RS_MOVE_TARGET_FILE_PATH_KEY");

    // Inner info classes

    public static class RsMoveMemberInfo implements RsMoveMemberSelection.RsMoveNodeInfo {
        @Nonnull
        private final RsItemElement member;

        public RsMoveMemberInfo(@Nonnull RsItemElement member) {
            this.member = member;
        }

        @Nonnull
        public RsItemElement getMember() {
            return member;
        }

        @Override
        public void render(@Nonnull ColoredTreeCellRenderer renderer) {
            String description;
            if (member instanceof RsModItem) {
                String modName = ((RsModItem) member).getName();
                description = RsBundle.message("mod.0", modName != null ? modName : "");
            } else {
                StringBuilder sb = new StringBuilder();
                RsDocumentationProvider.signature(member, sb);
                String descriptionHTML = sb.toString();
                String unescaped = StringEscapeUtils.unescapeHtml4(consulo.ui.ex.awt.util.StringHtmlUtil.removeHtmlTags(descriptionHTML));
                description = unescaped.replaceAll("(?U)\\s+", " ");
            }
            renderer.append(description, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        @Override
        public @Nonnull consulo.ui.image.Image getIcon() {
            return consulo.language.icon.IconDescriptorUpdaters.getIcon(member, 0);
        }
    }

    public static class RsMoveItemAndImplsInfo implements RsMoveMemberSelection.RsMoveNodeInfo {
        @Nonnull
        private final RsItemElement item;
        @Nonnull
        private final List<RsMoveMemberSelection.RsMoveNodeInfo> children;

        public RsMoveItemAndImplsInfo(@Nonnull RsItemElement item, @Nonnull List<RsImplItem> impls) {
            this.item = item;
            List<RsMoveMemberSelection.RsMoveNodeInfo> childList = new ArrayList<>();
            childList.add(new RsMoveMemberInfo(item));
            for (RsImplItem impl : impls) {
                childList.add(new RsMoveMemberInfo(impl));
            }
            this.children = childList;
        }

        @Override
        public void render(@Nonnull ColoredTreeCellRenderer renderer) {
            String name = item.getName();
            String keyword = null;
            if (item instanceof RsStructItem) keyword = RsBundle.message("struct");
            else if (item instanceof RsEnumItem) keyword = RsBundle.message("enum");
            else if (item instanceof RsTypeAlias) keyword = RsBundle.message("type2");
            else if (item instanceof RsTraitItem) keyword = RsBundle.message("trait");

            if (name == null || keyword == null) {
                renderer.append(RsBundle.message("item.and.impls"), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                return;
            }

            renderer.append(keyword + " ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
            renderer.append(name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            renderer.append(RsBundle.message("and.impls"), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        @Override
        @Nonnull
        public List<RsMoveMemberSelection.RsMoveNodeInfo> getChildren() {
            return children;
        }
    }
}
