/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInsight.intention.FileModifier;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.implementMembers.ImplementMembersImpl;
import org.rust.ide.utils.PsiInsertionPlace;
import org.rust.ide.utils.imports.ImportBridge;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.resolve.NameResolution;

import java.util.Objects;

public class ImplementDisplayFix extends RsQuickFixBase<RsStructOrEnumItemElement> {

    @Nls
    private final String _text;

    public ImplementDisplayFix(@NotNull RsStructOrEnumItemElement adt) {
        super(adt);
        _text = RsBundle.message("intention.name.implement.display.trait.for", Objects.requireNonNull(adt.getName()));
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return _text;
    }

    @NotNull
    @Override
    public String getText() {
        return _text;
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsStructOrEnumItemElement element) {
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
    public FileModifier getFileModifierForPreview(@NotNull PsiFile target) {
        return null;
    }

    @Override
    @NotNull
    public IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }
}
