/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.utils.imports.ImportInfo;
import org.rust.ide.utils.imports.ImportInfoUtil;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsPsiImplUtil;

/**
 * Fix that qualifies a path.
 */
public class QualifyPathFix extends RsQuickFixBase<RsPath> {

    @SafeFieldForPreview
    private final ImportInfo importInfo;

    public QualifyPathFix(@NotNull RsPath path, @NotNull ImportInfo importInfo) {
        super(path);
        this.importInfo = importInfo;
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.qualify.path.to", importInfo.getUsePath());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.qualify.path");
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsPath element) {
        qualify(element, importInfo);
    }

    public static void qualify(@NotNull RsPath path, @NotNull ImportInfo importInfo) {
        String qualifiedPath = importInfo.getUsePath();
        String typeArgText = path.getTypeArgumentList() != null ? path.getTypeArgumentList().getText() : "";
        String fullPath = qualifiedPath + typeArgText;
        RsPath newPath = new RsPsiFactory(path.getProject()).tryCreatePath(fullPath);
        if (newPath == null) return;

        if (!RsPsiImplUtil.isIntentionPreviewElement(path)) {
            ImportInfoUtil.insertExternCrateIfNeeded(importInfo, path);
        }
        path.replace(newPath);
    }
}
