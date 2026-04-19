/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.focusMode;

import com.intellij.codeInsight.daemon.impl.focusMode.FocusModeProvider;
import com.intellij.openapi.util.Segment;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SyntaxTraverser;
import org.rust.lang.core.psi.RsExternCrateItem;
import org.rust.lang.core.psi.RsMacro;
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.lang.core.psi.RsUseItem;
import org.rust.lang.core.psi.ext.RsItemElement;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@SuppressWarnings("UnstableApiUsage")
public class RsFocusModeProvider implements FocusModeProvider {

    @Override
    public List<? extends Segment> calcFocusZones(PsiFile file) {
        return StreamSupport.stream(
                SyntaxTraverser.psiTraverser(file).postOrderDfsTraversal().spliterator(), false)
            .filter(it -> it instanceof RsItemElement || it instanceof RsMacro)
            .filter(it -> !(it instanceof RsExternCrateItem) && !(it instanceof RsUseItem) && !(it instanceof RsModDeclItem))
            .map(it -> it.getTextRange())
            .collect(Collectors.toList());
    }
}
