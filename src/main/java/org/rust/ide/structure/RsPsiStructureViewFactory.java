/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.structure;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsFileBase;

public class RsPsiStructureViewFactory implements PsiStructureViewFactory {
    @Override
    @NotNull
    public StructureViewBuilder getStructureViewBuilder(@NotNull PsiFile psiFile) {
        RsFileBase rustFile = (RsFileBase) psiFile;
        return new TreeBasedStructureViewBuilder() {
            @Override
            @NotNull
            public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
                return new RsStructureViewModel(editor, rustFile);
            }
        };
    }
}
