/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import org.rust.RsBundle;
import org.rust.ide.refactoring.RsBaseEditorRefactoringAction;
import org.rust.ide.refactoring.extractEnumVariant.RsExtractEnumVariantAction;
import consulo.localize.LocalizeValue;

public class ExtractEnumVariantIntention extends RsRefactoringAdaptorIntention {

    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.extract.enum.variant"));
        }

    public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
        }

    @Override
    public RsBaseEditorRefactoringAction getRefactoringAction() {
        return new RsExtractEnumVariantAction();
    }
}
