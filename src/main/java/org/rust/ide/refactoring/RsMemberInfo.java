/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.refactoring.classMembers.MemberInfoBase;
import org.apache.commons.lang3.StringEscapeUtils;
import org.rust.ide.docs.RsDocumentationProvider;
import org.rust.lang.core.psi.RsModItem;
import org.rust.lang.core.psi.ext.RsItemElement;

public class RsMemberInfo extends MemberInfoBase<RsItemElement> {

    public RsMemberInfo(RsItemElement member, boolean isChecked) {
        super(member);
        setChecked(isChecked);
        if (member instanceof RsModItem) {
            displayName = "mod " + ((RsModItem) member).getModName();
        } else {
            StringBuilder sb = new StringBuilder();
            RsDocumentationProvider.signature(member, sb);
            String description = sb.toString();
            displayName = StringEscapeUtils.unescapeHtml4(StringUtil.removeHtmlTags(description));
        }
    }
}
