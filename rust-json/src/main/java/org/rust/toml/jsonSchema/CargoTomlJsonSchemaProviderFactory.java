/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.jsonSchema;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory;
import com.jetbrains.jsonSchema.extension.SchemaType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;

import java.util.Collections;
import java.util.List;

@ExtensionImpl
public class CargoTomlJsonSchemaProviderFactory implements JsonSchemaProviderFactory, DumbAware {
    static final String CARGO_TOML = "Cargo.toml";

    static boolean isCargoToml(@Nullable String fileName) {
        return CARGO_TOML.equals(fileName);
    }

    @Nonnull
    @Override
    public List<JsonSchemaFileProvider> getProviders(@Nonnull Project project) {
        return Collections.singletonList(new CargoTomlJsonSchemaFileProvider());
    }

    /**
     * Supplies the bundled {@code Cargo.toml} schema. Being embedded, it takes priority over the
     * remote one from schemastore.org.
     */
    public static class CargoTomlJsonSchemaFileProvider implements JsonSchemaFileProvider {
        public static final String SCHEMA_PATH = "/jsonSchema/cargo.toml-schema.json";

        @Override
        public boolean isAvailable(@Nonnull VirtualFile file) {
            return isCargoToml(file.getName());
        }

        @Nonnull
        @Override
        public String getName() {
            return RsBundle.message("cargo.toml.schema");
        }

        @Nonnull
        @Override
        public SchemaType getSchemaType() {
            return SchemaType.embeddedSchema;
        }


        @Nullable
        @Override
        public VirtualFile getSchemaFile() {
            return JsonSchemaProviderFactory.getResourceFile(CargoTomlJsonSchemaFileProvider.class, SCHEMA_PATH);
        }
    }
}
