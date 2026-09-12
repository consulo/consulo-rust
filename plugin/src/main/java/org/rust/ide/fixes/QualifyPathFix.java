/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.editor.inspection.FileModifier.SafeFieldForPreview;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.utils.imports.ImportInfo;
import org.rust.ide.utils.imports.ImportInfoUtil;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsPsiImplUtil;
import consulo.localize.LocalizeValue;

/**
 * Fix that qualifies a path.
 */
public class QualifyPathFix extends RsQuickFixBase<RsPath> {

    @SafeFieldForPreview
    private final ImportInfo importInfo;

    public QualifyPathFix(@Nonnull RsPath path, @Nonnull ImportInfo importInfo) {
        super(path);
        this.importInfo = importInfo;
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.qualify.path.to", importInfo.getUsePath()));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.qualify.path"));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsPath element) {
        qualify(element, importInfo);
    }

    public static void qualify(@Nonnull RsPath path, @Nonnull ImportInfo importInfo) {
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
