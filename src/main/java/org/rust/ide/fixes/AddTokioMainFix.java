/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProjectsUtil;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsOuterAttributeOwnerUtil;
import org.rust.openapiext.VirtualFileExtUtil;
import org.rust.toml.CargoDependencyUtil;
import org.rust.toml.CargoTomlPsiExtUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.RsPsiImplUtil;
import com.intellij.psi.PsiFile;

public class AddTokioMainFix extends RsQuickFixBase<RsFunction> {
    private static final List<String> REQUIRED_TOKIO_FEATURES = Arrays.asList("rt", "rt-multi-thread", "macros");

    public AddTokioMainFix(@NotNull RsFunction function) {
        super(function);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.name.add.tokio.main");
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.add.tokio.main");
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsFunction element) {
        if (!RsFunctionUtil.isAsync(element)) {
            com.intellij.psi.PsiElement anchor = element.getUnsafe();
            if (anchor == null) anchor = element.getExternAbi();
            if (anchor == null) anchor = element.getFn();
            element.addBefore(new RsPsiFactory(project).createAsyncKeyword(), anchor);
        }

        com.intellij.psi.PsiElement anchor = null;
        if (!element.getOuterAttrList().isEmpty()) {
            anchor = element.getOuterAttrList().get(0);
        }
        if (anchor == null) {
            anchor = RsFunctionUtil.getFirstKeyword(element);
        }
        org.rust.lang.core.psi.RsOuterAttr attr = new RsPsiFactory(project).createOuterAttr("tokio::main");
        element.addBefore(attr, anchor);

        if (!RsPsiImplUtil.isIntentionPreviewElement(element)) {
            org.rust.lang.core.crate.Crate crate = RsElementUtil.getContainingCrate(element);
            if (crate == null) return;
            org.toml.lang.psi.TomlFile tomlFile = org.rust.toml.CrateExt.getTomlFile(crate);
            if (tomlFile == null) return;
            List<String> requiredFeatures;
            if (org.rust.toml.Util.findDependencyFeatures(tomlFile, "tokio").contains("full")) {
                requiredFeatures = Collections.emptyList();
            } else {
                requiredFeatures = REQUIRED_TOKIO_FEATURES;
            }
            org.rust.toml.CrateExt.addCargoDependency(crate, "tokio", "1.0.0", requiredFeatures);
            com.intellij.openapi.vfs.VirtualFile vf = tomlFile.getVirtualFile();
            if (vf != null) {
                com.intellij.openapi.editor.Document document = VirtualFileExtUtil.getDocument(vf);
                if (document != null) {
                    FileDocumentManager.getInstance().saveDocument(document);
                }
            }

            CargoProjectsUtil.getCargoProjects(project).refreshAllProjects();
        }
    }
}
