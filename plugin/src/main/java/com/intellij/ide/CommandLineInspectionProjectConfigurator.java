package com.intellij.ide;

import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.nio.file.Path;

/** IntelliJ-compat stub for command-line inspection project configurators. */
public interface CommandLineInspectionProjectConfigurator {
    @Nonnull String getName();

    @Nonnull default String getDescription() { return getName(); }

    default boolean isApplicable(@Nonnull ConfiguratorContext context) { return false; }

    default void configureEnvironment(@Nonnull ConfiguratorContext context) {}

    default void preConfigureProject(@Nonnull Project project, @Nonnull ConfiguratorContext context) {}

    default void configureProject(@Nonnull Project project, @Nonnull ConfiguratorContext context) {}

    interface ConfiguratorContext {
        CommandLineInspectionProgressReporter getLogger();
        Project getProject();
        @Nonnull Path getProjectPath();
    }
}
