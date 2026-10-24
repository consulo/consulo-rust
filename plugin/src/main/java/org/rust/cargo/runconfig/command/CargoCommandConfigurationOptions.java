/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rust.cargo.runconfig.command;

import com.dslplatform.json.CompiledJson;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jspecify.annotations.Nullable;
import org.rust.cargo.runconfig.RsCommandConfigurationOptions;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
@CompiledJson
public class CargoCommandConfigurationOptions extends RsCommandConfigurationOptions {
    private @Nullable String myChannel;
    private boolean myRequiredFeatures = true;
    private boolean myAllFeatures;
    private boolean myWithSudo;
    private @Nullable String myBuildTarget;
    private @Nullable String myBacktrace;
    private boolean myRedirectInput;
    private @Nullable String myRedirectInputPath;

    public CargoCommandConfigurationOptions() {
        setCommand("run");
    }

    /**
     * The {@link org.rust.cargo.api.toolchain.RustChannel} name, {@code null} for the default channel.
     */
    @Attribute("channel")
    public @Nullable String getChannel() {
        return myChannel;
    }

    public void setChannel(@Nullable String channel) {
        myChannel = channel;
    }

    @Attribute("requiredFeatures")
    public boolean isRequiredFeatures() {
        return myRequiredFeatures;
    }

    public void setRequiredFeatures(boolean requiredFeatures) {
        myRequiredFeatures = requiredFeatures;
    }

    @Attribute("allFeatures")
    public boolean isAllFeatures() {
        return myAllFeatures;
    }

    public void setAllFeatures(boolean allFeatures) {
        myAllFeatures = allFeatures;
    }

    @Attribute("withSudo")
    public boolean isWithSudo() {
        return myWithSudo;
    }

    public void setWithSudo(boolean withSudo) {
        myWithSudo = withSudo;
    }

    /**
     * The {@link org.rust.cargo.runconfig.target.BuildTarget} name, {@code null} for the remote target.
     */
    @Attribute("buildTarget")
    public @Nullable String getBuildTarget() {
        return myBuildTarget;
    }

    public void setBuildTarget(@Nullable String buildTarget) {
        myBuildTarget = buildTarget;
    }

    /**
     * The {@link org.rust.cargo.api.toolchain.BacktraceMode} name, {@code null} for the short backtrace.
     */
    @Attribute("backtrace")
    public @Nullable String getBacktrace() {
        return myBacktrace;
    }

    public void setBacktrace(@Nullable String backtrace) {
        myBacktrace = backtrace;
    }

    @Attribute("redirectInput")
    public boolean isRedirectInput() {
        return myRedirectInput;
    }

    public void setRedirectInput(boolean redirectInput) {
        myRedirectInput = redirectInput;
    }

    @Attribute("redirectInputPath")
    public @Nullable String getRedirectInputPath() {
        return myRedirectInputPath;
    }

    public void setRedirectInputPath(@Nullable String redirectInputPath) {
        myRedirectInputPath = StringUtil.nullize(redirectInputPath, true);
    }
}
