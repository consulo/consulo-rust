/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import com.intellij.openapi.util.NlsContexts;
import com.intellij.refactoring.ui.AbstractMemberSelectionTable;
import com.intellij.refactoring.ui.MemberSelectionPanelBase;
import com.intellij.ui.RowIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsItemElement;

import javax.swing.*;
import java.util.List;

public class RsMemberSelectionPanel extends MemberSelectionPanelBase<
    RsItemElement,
    RsMemberInfo,
    AbstractMemberSelectionTable<RsItemElement, RsMemberInfo>
> {
    public RsMemberSelectionPanel(
        @NlsContexts.Separator @NotNull String title,
        @NotNull List<RsMemberInfo> memberInfo
    ) {
        super(title, new RsMemberSelectionTable(memberInfo));
    }
}
