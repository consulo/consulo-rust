/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.diagnostic;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.Experiments;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.io.URLUtil;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.settings.RustProjectSettingsService.MacroExpansionEngine;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.toolchain.impl.RustcVersion;
import org.rust.ide.experiments.EnabledInStable;
import org.rust.ide.experiments.RsExperiments;
import org.rust.lang.core.psi.RsFile;
import org.rust.openapiext.OpenApiUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class CreateNewGithubIssue extends DumbAwareAction {

    private static final String ISSUE_TEMPLATE =
        "<!--\n" +
        "Hello and thank you for the issue!\n" +
        "If you would like to report a bug, we have added some points below that you can fill out.\n" +
        "Feel free to remove all the irrelevant text to request a new feature.\n" +
        "-->\n\n" +
        "## Environment\n\n" +
        "%s\n\n" +
        "## Problem description\n\n\n" +
        "## Steps to reproduce\n" +
        "%s\n\n" +
        "<!--\n" +
        "Please include as much of your codebase as needed to reproduce the error.\n" +
        "If the relevant files are large, please provide a link to a public repository or a [Gist](https://gist.github.com/).\n" +
        "-->";

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null && org.rust.cargo.runconfig.RunConfigUtil.hasCargoProject(project));
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        String pluginVersion = OpenApiUtil.plugin().getVersion();
        String toolchainVersion = null;
        for (CargoProject cp : CargoProjectServiceUtil.getCargoProjects(project).getAllProjects()) {
            if (cp.getRustcInfo() != null && cp.getRustcInfo().getVersion() != null) {
                toolchainVersion = getDisplayText(cp.getRustcInfo().getVersion());
                break;
            }
        }
        String ideNameAndVersion = getIdeNameAndVersion();
        String os = SystemInfo.getOsNameAndVersion();
        String macroExpansionState = RsProjectSettingsServiceUtil.getRustSettings(project).getMacroExpansionEngine() == MacroExpansionEngine.DISABLED
            ? "disabled"
            : "enabled";
        String additionalExperimentalFeatures = getAdditionalExperimentalFeatures();
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        String codeSnippet = editor != null ? getCodeExample(editor) : "";

        String environmentInfo = buildEnvironmentInfo(
            pluginVersion, toolchainVersion, ideNameAndVersion, os, macroExpansionState, additionalExperimentalFeatures
        );
        String body = String.format(ISSUE_TEMPLATE, environmentInfo, codeSnippet);
        String link = "https://github.com/intellij-rust/intellij-rust/issues/new?body=" + URLUtil.encodeURIComponent(body);
        BrowserUtil.browse(link);
    }

    private static String buildEnvironmentInfo(
        String pluginVersion,
        String toolchainVersion,
        String ideNameAndVersion,
        String os,
        String macroExpansionState,
        String additionalExperimentalFeatures
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("* **IntelliJ Rust plugin version:** ").append(pluginVersion).append("\n");
        sb.append("* **Rust toolchain version:** ").append(toolchainVersion).append("\n");
        sb.append("* **IDE name and version:** ").append(ideNameAndVersion).append("\n");
        sb.append("* **Operating system:** ").append(os).append("\n");
        sb.append("* **Macro expansion:** ").append(macroExpansionState);
        if (additionalExperimentalFeatures != null) {
            sb.append("\n* **Additional experimental features:** ").append(additionalExperimentalFeatures);
        }
        return sb.toString();
    }

    private static String getIdeNameAndVersion() {
        ApplicationInfo appInfo = ApplicationInfo.getInstance();
        String appName = appInfo.getFullApplicationName();
        String editionName = ApplicationNamesInfo.getInstance().getEditionName();
        String ideVersion = appInfo.getBuild().toString();
        StringBuilder sb = new StringBuilder();
        sb.append(appName);
        if (editionName != null) {
            sb.append(" ").append(editionName);
        }
        sb.append(" (").append(ideVersion).append(")");
        return sb.toString();
    }

    private static String getAdditionalExperimentalFeatures() {
        Experiments experiments = Experiments.getInstance();
        List<String> features = new ArrayList<>();
        for (Field field : RsExperiments.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(EnabledInStable.class)) continue;
            if (field.getType() != String.class) continue;
            try {
                String featureId = (String) field.get(null);
                if (experiments.isFeatureEnabled(featureId)) {
                    features.add(featureId);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return features.isEmpty() ? null : String.join(", ", features);
    }

    private static String getDisplayText(RustcVersion version) {
        StringBuilder sb = new StringBuilder();
        sb.append(version.getSemver().getParsedVersion());
        if (version.getCommitHash() != null) {
            sb.append(" (");
            sb.append(version.getCommitHash().substring(0, Math.min(9, version.getCommitHash().length())));
            if (version.getCommitDate() != null) {
                sb.append(" ").append(version.getCommitDate());
            }
            sb.append(")");
        }
        sb.append(" ").append(version.getHost());
        return sb.toString();
    }

    private static String getCodeExample(Editor editor) {
        var vFile = OpenApiUtil.getVirtualFile(editor.getDocument());
        if (vFile == null || !RsFile.isRustFile(vFile)) return "";
        String selectedCode = editor.getSelectionModel().getSelectedText();
        if (selectedCode == null) return "";
        return "```rust\n" + selectedCode + "\n```";
    }
}
