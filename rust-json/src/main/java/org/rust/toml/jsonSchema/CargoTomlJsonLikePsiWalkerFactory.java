/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.jsonSchema;

import com.jetbrains.jsonSchema.JsonSchemaObject;
import com.jetbrains.jsonSchema.walker.JsonLikePsiWalker;
import com.jetbrains.jsonSchema.walker.JsonLikePsiWalkerFactory;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nullable;
import org.toml.lang.psi.TomlFile;

/**
 * Hands the JSON schema engine a TOML walker for {@code Cargo.toml}. Other TOML files are left
 * alone, since only the manifest is the Rust plugin's to describe.
 */
@ExtensionImpl
public class CargoTomlJsonLikePsiWalkerFactory implements JsonLikePsiWalkerFactory {
    @Override
    public boolean handles(PsiElement element) {
        PsiFile file = element.getContainingFile();
        return file instanceof TomlFile && CargoTomlJsonSchemaProviderFactory.isCargoToml(file.getName());
    }

    @Override
    public JsonLikePsiWalker create(@Nullable JsonSchemaObject schemaObject) {
        return CargoTomlJsonPsiWalker.INSTANCE;
    }
}
