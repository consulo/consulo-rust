/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.jsonSchema;

import com.jetbrains.jsonSchema.JsonComplianceCheckerOptions;
import com.jetbrains.jsonSchema.JsonSchemaObject;
import com.jetbrains.jsonSchema.JsonSchemaService;
import com.jetbrains.jsonSchema.impl.JsonSchemaComplianceChecker;
import com.jetbrains.jsonSchema.impl.inspections.JsonSchemaComplianceInspectionState;
import com.jetbrains.jsonSchema.walker.JsonLikePsiWalker;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.InspectionToolState;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.toml.lang.TomlLanguage;
import org.toml.lang.psi.TomlFile;

import java.util.Collection;

/**
 * Checks {@code Cargo.toml} against its schema.
 * <p>
 * The schema engine only provides the checker; each language needs an inspection to run it, and
 * TOML has none of its own, so this one drives it for the manifest.
 */
@ExtensionImpl
public class CargoTomlSchemaValidationInspection extends LocalInspectionTool {
    @Nullable
    @Override
    public Language getLanguage() {
        return TomlLanguage.INSTANCE;
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "CargoTomlSchemaValidation";
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.of(RsBundle.message("inspection.cargo.toml.schema.validation.display.name"));
    }

    @Nonnull
    @Override
    public LocalizeValue getGroupDisplayName() {
        return LocalizeValue.of(RsBundle.message("cargo.toml"));
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Nonnull
    @Override
    public InspectionToolState<?> createStateProvider() {
        return new JsonSchemaComplianceInspectionState();
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder,
                                          boolean isOnTheFly,
                                          @Nonnull LocalInspectionToolSession session,
                                          @Nonnull Object state) {
        PsiFile file = holder.getFile();
        if (!(file instanceof TomlFile) || !CargoTomlJsonSchemaProviderFactory.isCargoToml(file.getName())) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }
        Collection<PsiElement> roots = CargoTomlJsonPsiWalker.INSTANCE.getRoots(file);
        if (roots.isEmpty()) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }
        JsonSchemaService service = JsonSchemaService.get(file.getProject());
        VirtualFile virtualFile = file.getViewProvider().getVirtualFile();
        if (!service.isApplicableToFile(virtualFile)) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }
        JsonSchemaObject rootSchema = service.getSchemaObject(file);
        if (rootSchema == null) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        JsonComplianceCheckerOptions options =
            new JsonComplianceCheckerOptions(((JsonSchemaComplianceInspectionState) state).myCaseInsensitiveEnum);
        String prefix = RsBundle.message("inspection.cargo.toml.schema.validation.prefix") + " ";
        return new PsiElementVisitor() {
            private boolean myChecked;

            @Override
            @RequiredReadAction
            public void visitFile(PsiFile psiFile) {
                check(psiFile);
            }

            @Override
            @RequiredReadAction
            public void visitElement(PsiElement element) {
                if (element instanceof PsiFile psiFile) {
                    check(psiFile);
                }
            }

            /**
             * The whole manifest is one object, so it is checked once, from the file.
             */
            @RequiredReadAction
            private void check(PsiFile psiFile) {
                if (myChecked || !roots.contains(psiFile)) {
                    return;
                }
                myChecked = true;
                JsonLikePsiWalker walker = JsonLikePsiWalker.getWalker(psiFile, rootSchema);
                if (walker != null) {
                    new JsonSchemaComplianceChecker(rootSchema, holder, walker, session, options, prefix).annotate(psiFile);
                }
            }
        };
    }
}
