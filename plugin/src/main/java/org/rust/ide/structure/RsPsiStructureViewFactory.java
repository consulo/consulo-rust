/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.structure;

import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.fileEditor.structureView.TreeBasedStructureViewBuilder;
import consulo.language.editor.structureView.PsiStructureViewFactory;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsFileBase;

public class RsPsiStructureViewFactory implements PsiStructureViewFactory {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }

    @Override
    @Nonnull
    public StructureViewBuilder getStructureViewBuilder(@Nonnull PsiFile psiFile) {
        RsFileBase rustFile = (RsFileBase) psiFile;
        return new TreeBasedStructureViewBuilder() {
            @Override
            @Nonnull
            public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
                return new RsStructureViewModel(editor, rustFile);
            }
        };
    }
}
