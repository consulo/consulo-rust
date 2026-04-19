/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.tools;

import com.intellij.openapi.project.Project;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.settings.RsExternalLinterProjectSettingsService;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.ExternalLinter;
import org.rust.cargo.toolchain.RustChannel;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

// Note: the class is used as a cache key, so it must be immutable and must have a correct equals/hashCode
public abstract class CargoCheckArgs {

    public abstract ExternalLinter getLinter();
    public abstract Path getCargoProjectDirectory();
    public abstract String getExtraArguments();
    public abstract RustChannel getChannel();
    public abstract Map<String, String> getEnvs();

    public static final class SpecificTarget extends CargoCheckArgs {
        private final ExternalLinter myLinter;
        private final Path myCargoProjectDirectory;
        private final CargoWorkspace.Target myTarget;
        private final String myExtraArguments;
        private final RustChannel myChannel;
        private final Map<String, String> myEnvs;

        public SpecificTarget(
            ExternalLinter linter,
            Path cargoProjectDirectory,
            CargoWorkspace.Target target,
            String extraArguments,
            RustChannel channel,
            Map<String, String> envs
        ) {
            myLinter = linter;
            myCargoProjectDirectory = cargoProjectDirectory;
            myTarget = target;
            myExtraArguments = extraArguments;
            myChannel = channel;
            myEnvs = envs;
        }

        @Override public ExternalLinter getLinter() { return myLinter; }
        @Override public Path getCargoProjectDirectory() { return myCargoProjectDirectory; }
        public CargoWorkspace.Target getTarget() { return myTarget; }
        @Override public String getExtraArguments() { return myExtraArguments; }
        @Override public RustChannel getChannel() { return myChannel; }
        @Override public Map<String, String> getEnvs() { return myEnvs; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SpecificTarget)) return false;
            SpecificTarget that = (SpecificTarget) o;
            return myLinter == that.myLinter &&
                Objects.equals(myCargoProjectDirectory, that.myCargoProjectDirectory) &&
                Objects.equals(myTarget, that.myTarget) &&
                Objects.equals(myExtraArguments, that.myExtraArguments) &&
                myChannel == that.myChannel &&
                Objects.equals(myEnvs, that.myEnvs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myLinter, myCargoProjectDirectory, myTarget, myExtraArguments, myChannel, myEnvs);
        }
    }

    public static final class FullWorkspace extends CargoCheckArgs {
        private final ExternalLinter myLinter;
        private final Path myCargoProjectDirectory;
        private final boolean myAllTargets;
        private final String myExtraArguments;
        private final RustChannel myChannel;
        private final Map<String, String> myEnvs;

        public FullWorkspace(
            ExternalLinter linter,
            Path cargoProjectDirectory,
            boolean allTargets,
            String extraArguments,
            RustChannel channel,
            Map<String, String> envs
        ) {
            myLinter = linter;
            myCargoProjectDirectory = cargoProjectDirectory;
            myAllTargets = allTargets;
            myExtraArguments = extraArguments;
            myChannel = channel;
            myEnvs = envs;
        }

        @Override public ExternalLinter getLinter() { return myLinter; }
        @Override public Path getCargoProjectDirectory() { return myCargoProjectDirectory; }
        public boolean getAllTargets() { return myAllTargets; }
        @Override public String getExtraArguments() { return myExtraArguments; }
        @Override public RustChannel getChannel() { return myChannel; }
        @Override public Map<String, String> getEnvs() { return myEnvs; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FullWorkspace)) return false;
            FullWorkspace that = (FullWorkspace) o;
            return myAllTargets == that.myAllTargets &&
                myLinter == that.myLinter &&
                Objects.equals(myCargoProjectDirectory, that.myCargoProjectDirectory) &&
                Objects.equals(myExtraArguments, that.myExtraArguments) &&
                myChannel == that.myChannel &&
                Objects.equals(myEnvs, that.myEnvs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myLinter, myCargoProjectDirectory, myAllTargets, myExtraArguments, myChannel, myEnvs);
        }
    }

    public static CargoCheckArgs forTarget(Project project, CargoWorkspace.Target target) {
        RsExternalLinterProjectSettingsService settings = RsProjectSettingsServiceUtil.getExternalLinterSettings(project);
        return new SpecificTarget(
            settings.getTool(),
            target.getPkg().getWorkspace().getContentRoot(),
            target,
            settings.getAdditionalArguments(),
            settings.getChannel(),
            settings.getEnvs()
        );
    }

    public static CargoCheckArgs forCargoProject(CargoProject cargoProject) {
        RsExternalLinterProjectSettingsService settings = RsProjectSettingsServiceUtil.getExternalLinterSettings(cargoProject.getProject());
        return new FullWorkspace(
            settings.getTool(),
            CargoCommandConfiguration.getWorkingDirectory(cargoProject),
            RsProjectSettingsServiceUtil.getRustSettings(cargoProject.getProject()).getCompileAllTargets(),
            settings.getAdditionalArguments(),
            settings.getChannel(),
            settings.getEnvs()
        );
    }
}
