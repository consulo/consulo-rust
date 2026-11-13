/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.jsonSchema;

import com.jetbrains.jsonSchema.extension.JsonSchemaEnabler;
import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;
import org.toml.lang.psi.TomlFileType;

/**
 * Turns JSON schema support on for {@code Cargo.toml}, which the engine would otherwise skip as a
 * non-JSON file.
 */
@ExtensionImpl
public class CargoTomlJsonSchemaEnabler implements JsonSchemaEnabler {
    @Override
    public boolean isEnabledForFile(VirtualFile file, @Nullable Project project) {
        return file.getFileType() instanceof TomlFileType && CargoTomlJsonSchemaProviderFactory.isCargoToml(file.getName());
    }
}
