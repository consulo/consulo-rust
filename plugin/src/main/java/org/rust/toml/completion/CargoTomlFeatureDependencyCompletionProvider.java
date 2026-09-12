/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.toml.Util;
import org.rust.toml.resolve.CargoTomlNameResolution;
import org.toml.lang.psi.TomlFile;
import org.toml.lang.psi.TomlKeySegment;
import org.toml.lang.psi.impl.TomlKeyValueImpl;

import java.util.List;
import consulo.language.psi.PsiElement;

/**
 * Provides completion for feature dependencies in [features] section
 */
public class CargoTomlFeatureDependencyCompletionProvider implements CompletionProvider {

    @Override
    public void addCompletions(@Nonnull CompletionParameters parameters,
                                  @Nonnull ProcessingContext context,
                                  @Nonnull CompletionResultSet result) {
        consulo.language.psi.PsiElement element = parameters.getPosition();
        if (!(element.getContainingFile() instanceof TomlFile)) return;
        TomlFile tomlFile = (TomlFile) element.getContainingFile();

        TomlKeyValueImpl parentKv = PsiTreeUtil.getParentOfType(element, TomlKeyValueImpl.class);
        if (parentKv == null) return;
        List<TomlKeySegment> segments = parentKv.getKey().getSegments();
        if (segments.size() != 1) return;
        TomlKeySegment parentFeature = segments.get(0);

        for (TomlKeySegment feature : CargoTomlNameResolution.allFeatures(tomlFile, false)) {
            if (feature.equals(parentFeature)) continue;
            result.addElement(CargoTomlLookupElements.lookupElementForFeature(feature));
        }

        CargoWorkspace.Package pkg = Util.findCargoPackageForCargoToml(tomlFile);
        if (pkg == null) return;
        for (CargoWorkspace.Dependency dep : pkg.getDependencies()) {
            if (dep.getPkg().getOrigin() == PackageOrigin.STDLIB) continue;
            TomlFile depToml = Util.getPackageCargoTomlFile(dep.getPkg(), tomlFile.getProject());
            if (depToml == null) continue;
            for (TomlKeySegment feature : CargoTomlNameResolution.allFeatures(depToml, false)) {
                result.addElement(
                    LookupElementBuilder
                        .createWithSmartPointer(dep.getPkg().getName() + "/" + feature.getText(), feature)
                        .withInsertHandler(new Util.StringLiteralInsertionHandler())
                );
            }
        }
    }
}
