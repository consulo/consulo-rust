package com.intellij.execution.target;

import consulo.project.Project;

import java.util.ArrayList;
import java.util.List;

/** Named execution-target configuration holding the language runtimes it provides. */
public abstract class TargetEnvironmentConfiguration {
    private String displayName = "";
    private final Runtimes runtimes = new Runtimes();

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String name) { this.displayName = name; }
    public String getTypeId() { return ""; }

    public Runtimes getRuntimes() { return runtimes; }

    public TargetEnvironmentRequest createEnvironmentRequest(Project project) {
        return new TargetEnvironmentRequest() {};
    }

    public static final class Runtimes {
        private final List<LanguageRuntimeConfiguration> runtimes = new ArrayList<>();
        public void addRuntime(LanguageRuntimeConfiguration r) { runtimes.add(r); }
        public <T extends LanguageRuntimeConfiguration> T findByType(Class<T> clazz) {
            for (LanguageRuntimeConfiguration r : runtimes) {
                if (clazz.isInstance(r)) return clazz.cast(r);
            }
            return null;
        }
        public List<LanguageRuntimeConfiguration> resolvedConfigs() { return runtimes; }
    }
}
