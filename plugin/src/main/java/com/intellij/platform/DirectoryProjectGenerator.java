package com.intellij.platform;

import com.intellij.facet.ui.ValidationResult;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/** Generates project content into a directory from the new-project wizard. */
public interface DirectoryProjectGenerator<T> {
    @Nonnull String getName();
    @Nullable default Image getLogo() { return null; }
    default void generateProject(@Nonnull Project project, @Nonnull VirtualFile baseDir, @Nonnull T settings, @Nonnull Module module) {}
    @Nonnull default ValidationResult validate(@Nonnull String baseDirPath) { return ValidationResult.OK; }
}
