package com.jetbrains.jsonSchema.extension;
import consulo.project.Project;
import java.util.List;
import consulo.virtualFileSystem.VirtualFile;
public interface JsonSchemaProviderFactory {
    List<JsonSchemaFileProvider> getProviders(Project project);

    static consulo.virtualFileSystem.VirtualFile getResourceFile(Class<?> providerClass, String resourcePath) {
        // resource lookup is not implemented, so no bundled schema file is returned
        return null;
    }
}
