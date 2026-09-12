/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.target;

import com.intellij.execution.target.LanguageRuntimeConfiguration;
import com.intellij.openapi.components.BaseState;
import consulo.component.persist.PersistentStateComponent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class RsLanguageRuntimeConfiguration extends LanguageRuntimeConfiguration
    implements PersistentStateComponent<RsLanguageRuntimeConfiguration.MyState> {

    private String myRustcPath = "";
    private String myRustcVersion = "";
    private String myCargoPath = "";
    private String myCargoVersion = "";
    private String myLocalBuildArgs = "";

    public RsLanguageRuntimeConfiguration() {
        super(RsLanguageRuntimeType.TYPE_ID);
    }

    @Nonnull
    public String getRustcPath() {
        return myRustcPath;
    }

    public void setRustcPath(@Nonnull String rustcPath) {
        myRustcPath = rustcPath;
    }

    @Nonnull
    public String getRustcVersion() {
        return myRustcVersion;
    }

    public void setRustcVersion(@Nonnull String rustcVersion) {
        myRustcVersion = rustcVersion;
    }

    @Nonnull
    public String getCargoPath() {
        return myCargoPath;
    }

    public void setCargoPath(@Nonnull String cargoPath) {
        myCargoPath = cargoPath;
    }

    @Nonnull
    public String getCargoVersion() {
        return myCargoVersion;
    }

    public void setCargoVersion(@Nonnull String cargoVersion) {
        myCargoVersion = cargoVersion;
    }

    @Nonnull
    public String getLocalBuildArgs() {
        return myLocalBuildArgs;
    }

    public void setLocalBuildArgs(@Nonnull String localBuildArgs) {
        myLocalBuildArgs = localBuildArgs;
    }

    @Nonnull
    @Override
    public MyState getState() {
        MyState state = new MyState();
        state.setRustcPath(myRustcPath);
        state.setRustcVersion(myRustcVersion);
        state.setCargoPath(myCargoPath);
        state.setCargoVersion(myCargoVersion);
        state.setLocalBuildArgs(myLocalBuildArgs);
        return state;
    }

    @Override
    public void loadState(@Nonnull MyState state) {
        myRustcPath = state.getRustcPath() != null ? state.getRustcPath() : "";
        myRustcVersion = state.getRustcVersion() != null ? state.getRustcVersion() : "";
        myCargoPath = state.getCargoPath() != null ? state.getCargoPath() : "";
        myCargoVersion = state.getCargoVersion() != null ? state.getCargoVersion() : "";
        myLocalBuildArgs = state.getLocalBuildArgs() != null ? state.getLocalBuildArgs() : "";
    }

    public static class MyState extends BaseState {
        private String myRustcPath;
        private String myRustcVersion;
        private String myCargoPath;
        private String myCargoVersion;
        private String myLocalBuildArgs;

        @Nullable
        public String getRustcPath() {
            return myRustcPath;
        }

        public void setRustcPath(@Nullable String rustcPath) {
            myRustcPath = rustcPath;
        }

        @Nullable
        public String getRustcVersion() {
            return myRustcVersion;
        }

        public void setRustcVersion(@Nullable String rustcVersion) {
            myRustcVersion = rustcVersion;
        }

        @Nullable
        public String getCargoPath() {
            return myCargoPath;
        }

        public void setCargoPath(@Nullable String cargoPath) {
            myCargoPath = cargoPath;
        }

        @Nullable
        public String getCargoVersion() {
            return myCargoVersion;
        }

        public void setCargoVersion(@Nullable String cargoVersion) {
            myCargoVersion = cargoVersion;
        }

        @Nullable
        public String getLocalBuildArgs() {
            return myLocalBuildArgs;
        }

        public void setLocalBuildArgs(@Nullable String localBuildArgs) {
            myLocalBuildArgs = localBuildArgs;
        }
    }
}
