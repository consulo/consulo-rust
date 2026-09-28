/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.type;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.language.editor.inlay.InlayActionHandler;
import consulo.language.editor.inlay.InlayActionPayload;
import consulo.language.psi.PsiElement;
import consulo.navigation.Navigatable;

@ExtensionImpl
public class RsInlayActionHandler implements InlayActionHandler {

    public static final String HANDLER_ID = "rust.type.declaration";

    @Override
    public String getHandlerId() {
        return HANDLER_ID;
    }

    @Override
    public void handleClick(EditorMouseEvent e, InlayActionPayload payload) {
        if (!(payload instanceof InlayActionPayload.PsiPointerInlayActionPayload pointerPayload)) {
            return;
        }
        PsiElement element = pointerPayload.getPointer().getElement();
        if (element instanceof Navigatable navigatable && navigatable.canNavigate()) {
            navigatable.navigate(true);
        }
    }
}
