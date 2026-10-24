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
package org.rust.cargo.runconfig;

import com.dslplatform.json.CompiledJson;
import consulo.execution.configuration.RunConfigurationOptions;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
@CompiledJson
public class RsCommandConfigurationOptions extends RunConfigurationOptions {
    private @Nullable String myCommand;
    private @Nullable String myWorkingDirectory;
    private @Nullable Boolean myEmulateTerminal;

    @Attribute("command")
    public @Nullable String getCommand() {
        return myCommand;
    }

    public void setCommand(@Nullable String command) {
        myCommand = command;
    }

    /**
     * System-independent path of the directory cargo runs in.
     */
    @Attribute("workingDirectory")
    public @Nullable String getWorkingDirectory() {
        return myWorkingDirectory;
    }

    public void setWorkingDirectory(@Nullable String workingDirectory) {
        myWorkingDirectory = StringUtil.nullize(workingDirectory, true);
    }

    /**
     * {@code null} for the default of the IDE.
     */
    @Attribute("emulateTerminal")
    public @Nullable Boolean getEmulateTerminal() {
        return myEmulateTerminal;
    }

    public void setEmulateTerminal(@Nullable Boolean emulateTerminal) {
        myEmulateTerminal = emulateTerminal;
    }
}
