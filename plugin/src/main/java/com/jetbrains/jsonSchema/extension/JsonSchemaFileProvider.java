package com.jetbrains.jsonSchema.extension;
import consulo.virtualFileSystem.VirtualFile;
public interface JsonSchemaFileProvider {
    boolean isAvailable(VirtualFile file);
    String getName();
    VirtualFile getSchemaFile();
    SchemaType getSchemaType();
}
