/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.rust.RsBundle;
import org.rust.ide.utils.StructFieldsExpander;
import org.rust.lang.core.psi.RsPsiFactory;

public class AddStructFieldsLiteralRecursiveIntention extends AddStructFieldsLiteralIntention {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.recursively.replace.with.actual.fields");
    }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        removeDotsAndBaseStruct(ctx.structLiteral);
        StructFieldsExpander.addMissingFieldsToStructLiteral(new RsPsiFactory(project), editor, ctx.structLiteral, true);
    }
}
