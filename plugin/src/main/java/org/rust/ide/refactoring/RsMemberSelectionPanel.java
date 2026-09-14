/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;


import consulo.language.editor.refactoring.ui.AbstractMemberSelectionTable;
import consulo.language.editor.refactoring.ui.MemberSelectionPanelBase;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsItemElement;

import java.util.List;

public class RsMemberSelectionPanel extends MemberSelectionPanelBase<
    RsItemElement,
    RsMemberInfo,
    AbstractMemberSelectionTable<RsItemElement, RsMemberInfo>
> {
    public RsMemberSelectionPanel(
         @Nonnull String title,
        @Nonnull List<RsMemberInfo> memberInfo
    ) {
        super(title, new RsMemberSelectionTable(memberInfo));
    }
}
