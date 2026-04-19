/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import com.intellij.refactoring.ui.AbstractMemberSelectionTable;
import com.intellij.ui.RowIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsItemElement;

import javax.swing.*;
import java.util.List;

public class RsMemberSelectionTable extends AbstractMemberSelectionTable<RsItemElement, RsMemberInfo> {

    public RsMemberSelectionTable(@NotNull List<RsMemberInfo> memberInfo) {
        super(memberInfo, null, null);
        setTableHeader(null);
    }

    @Nullable
    @Override
    protected Object getAbstractColumnValue(@Nullable RsMemberInfo memberInfo) {
        return null;
    }

    @Override
    protected boolean isAbstractColumnEditable(int rowIndex) {
        return false;
    }

    @Override
    protected void setVisibilityIcon(@NotNull RsMemberInfo memberInfo, @NotNull RowIcon icon) {
        // we don't set visibility icon
    }

    @Nullable
    @Override
    protected Icon getOverrideIcon(@NotNull RsMemberInfo memberInfo) {
        return null;
    }
}
