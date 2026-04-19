/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.jsonSchema;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory;
import com.jetbrains.jsonSchema.extension.SchemaType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;

import java.util.Collections;
import java.util.List;

public class CargoTomlJsonSchemaProviderFactory implements JsonSchemaProviderFactory, DumbAware {
    @NotNull
    @Override
    public List<JsonSchemaFileProvider> getProviders(@NotNull Project project) {
        return Collections.singletonList(new CargoTomlJsonSchemaFileProvider());
    }

    /**
     * Provides empty json schema for Cargo.toml files.
     * It's a temporary hack not to use remote scheme for Cargo.toml from https://json.schemastore.org/cargo.json
     * by providing own empty embedded scheme (embedded schemes have more priority than remote ones)
     * because it suggests unexpected completion variants like {@code {}}.
     */
    public static class CargoTomlJsonSchemaFileProvider implements JsonSchemaFileProvider {
        public static final String SCHEMA_PATH = "/jsonSchema/cargo.toml-schema.json";

        @Override
        public boolean isAvailable(@NotNull VirtualFile file) {
            return "Cargo.toml".equals(file.getName());
        }

        @NotNull
        @Override
        public String getName() {
            return RsBundle.message("cargo.toml.schema");
        }

        @NotNull
        @Override
        public SchemaType getSchemaType() {
            return SchemaType.embeddedSchema;
        }

        @Override
        public boolean isUserVisible() {
            return false;
        }

        @Nullable
        @Override
        public VirtualFile getSchemaFile() {
            return JsonSchemaProviderFactory.getResourceFile(CargoTomlJsonSchemaFileProvider.class, SCHEMA_PATH);
        }
    }
}
