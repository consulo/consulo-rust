package com.intellij.execution.target;

import java.util.ArrayList;
import java.util.List;

public interface TargetEnvironmentRequest {
    default List<TargetEnvironment.UploadRoot> getUploadVolumes() { return new ArrayList<>(); }
    default void setProjectPathOnTarget(String path) {}
    default String getProjectPathOnTarget() { return ""; }
    default TargetEnvironmentConfiguration getConfiguration() { return null; }
    default TargetEnvironment prepareEnvironment(Object progressIndicator) throws Exception {
        throw new UnsupportedOperationException("target environment prepare not implemented in consulo stub");
    }
}
