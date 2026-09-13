/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import org.rust.RsBundle;
import org.rust.ide.utils.StructFieldsExpander;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import consulo.localize.LocalizeValue;

public class AddStructFieldsLiteralRecursiveIntention extends AddStructFieldsLiteralIntention {

    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.recursively.replace.with.actual.fields"));
        }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        removeDotsAndBaseStruct(ctx.structLiteral);
        StructFieldsExpander.addMissingFieldsToStructLiteral(new RsPsiFactory(project), editor, ctx.structLiteral, true);
    }
}
