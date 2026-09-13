package com.intellij.execution.target;

import consulo.component.persist.PersistentStateComponent;
import consulo.configurable.Configurable;
import consulo.project.Project;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Icon;
import java.util.function.Supplier;
import consulo.component.extension.ExtensionPointName;

/** Runtime type of a language on an execution target: its configuration, UI and introspection. */
public abstract class LanguageRuntimeType<T extends LanguageRuntimeConfiguration> {
    public static final ExtensionPointName<LanguageRuntimeType<?>> EXTENSION_NAME =
        (ExtensionPointName) ExtensionPointName.create(LanguageRuntimeType.class);

    private final String id;
    protected LanguageRuntimeType(String id) { this.id = id; }
    public String getId() { return id; }

    @Nonnull public abstract String getDisplayName();
    @Nonnull public abstract Image getIcon();
    @Nonnull public abstract String getConfigurableDescription();
    @Nonnull public abstract String getLaunchDescription();
    @Nonnull public abstract PersistentStateComponent<?> createSerializer(@Nonnull T config);
    @Nonnull public abstract T createDefaultConfig();
    @Nonnull public T duplicateConfig(@Nonnull T config) { return config; }
    @Nullable public Introspector<T> createIntrospector(@Nonnull T config) { return null; }
    @Nonnull public abstract Configurable createConfigurable(@Nonnull Project project,
                                                             @Nonnull T config,
                                                             @Nonnull TargetEnvironmentType<?> targetEnvironmentType,
                                                             @Nonnull Supplier<TargetEnvironmentConfiguration> targetSupplier);

    protected static <C extends PersistentStateComponent<?>> C duplicatePersistentComponent(Object owner, C original) { return original; }

    public static class VolumeDescriptor {
        public final String id;
        public final String defaultPath;
        public final String name;
        public final String description;
        public VolumeDescriptor(String id, String name, String description, String defaultPath) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.defaultPath = defaultPath;
        }
        public String getId() { return id; }
        public String getDefaultPath() { return defaultPath; }
        public String getWizardLabel() { return name; }
        public String getDescription() { return description; }
    }

    public interface Introspectable {
        java.util.concurrent.CompletableFuture<String> promiseExecuteScript(java.util.List<String> script);

        default java.util.concurrent.CompletableFuture<String> promiseExecuteScript(String script) {
            return promiseExecuteScript(java.util.List.of(script));
        }
    }

    public interface Introspector<T> {
        java.util.concurrent.CompletableFuture<T> introspect(Introspectable subject);
    }
}
