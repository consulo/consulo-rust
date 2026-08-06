package com.jetbrains.jsonSchema.extension;
import consulo.project.Project;
import java.util.List;
public interface JsonSchemaProviderFactory {
    List<JsonSchemaFileProvider> getProviders(Project project);

    static consulo.virtualFileSystem.VirtualFile getResourceFile(Class<?> providerClass, String resourcePath) {
        // IntelliJ-compat stub: resource lookup not wired into Consulo here
        return null;
    }
}
