/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.editor.inspection.FileModifier;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.implementMembers.ImplementMembersImpl;
import org.rust.ide.utils.PsiInsertionPlace;
import org.rust.lang.core.imports.ImportBridge;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.resolve.NameResolution;

import java.util.Objects;
import consulo.localize.LocalizeValue;

public class ImplementDisplayFix extends RsQuickFixBase<RsStructOrEnumItemElement> {

    
    private final String _text;

    public ImplementDisplayFix(@Nonnull RsStructOrEnumItemElement adt) {
        super(adt);
        _text = RsBundle.message("intention.name.implement.display.trait.for", Objects.requireNonNull(adt.getName()));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(_text);
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(_text);
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsStructOrEnumItemElement element) {
        var knownItems = KnownItems.knownItems(element);
        RsTraitItem display = knownItems.getDisplay();
        if (display == null) return;

        var found = NameResolution.findInScope(element, "Display", NameResolution.getTYPES());
        String displayName;
        if (found == display) {
            displayName = "Display";
        } else if (found == null) {
            ImportBridge.importElement(element, display);
            displayName = "Display";
        } else {
            displayName = "std::fmt::Display";
        }

        PsiInsertionPlace placeForImpl = PsiInsertionPlace.forItemInTheScopeOf(element);
        if (placeForImpl == null) return;
        RsPsiFactory psiFactory = new RsPsiFactory(project);
        String name = element.getName();
        if (name == null) return;
        RsImplItem createdImpl = psiFactory.createTraitImplItem(
            name,
            displayName,
            element.getTypeParameterList(),
            element.getWhereClause()
        );
        RsImplItem insertedImpl = (RsImplItem) placeForImpl.insert(createdImpl);

        if (insertedImpl.getTraitRef() != null) {
            ImplementMembersImpl.generateMissingTraitMembers(insertedImpl, insertedImpl.getTraitRef(), editor);
        }
    }

    @Override
    @Nullable
    public FileModifier getFileModifierForPreview(@Nonnull PsiFile target) {
        return null;
    }

    @Nonnull
    public IntentionPreviewInfo generatePreview(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }
}
