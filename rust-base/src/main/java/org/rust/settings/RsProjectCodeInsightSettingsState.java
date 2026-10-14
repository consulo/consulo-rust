package org.rust.settings;

import com.dslplatform.json.CompiledJson;
import jakarta.annotation.Nonnull;

import java.util.Arrays;

/**
* @author VISTALL
* @since 2026-10-14
*/
@CompiledJson
public class RsProjectCodeInsightSettingsState {
    private ExcludedPath[] excludedPaths = new ExcludedPath[0];

    @Nonnull
    public ExcludedPath[] getExcludedPaths() {
        return excludedPaths;
    }

    public void setExcludedPaths(@Nonnull ExcludedPath[] excludedPaths) {
        this.excludedPaths = excludedPaths;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RsProjectCodeInsightSettingsState that = (RsProjectCodeInsightSettingsState) o;
        return Arrays.equals(excludedPaths, that.excludedPaths);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(excludedPaths);
    }
}
