package vektah.rust.ide.runner;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.google.common.base.Strings;
import com.intellij.execution.CommonProgramRunConfigurationParameters;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.XmlSerializer;
import vektah.rust.i18n.RustBundle;

public class RustConfiguration extends ModuleBasedConfiguration<RunConfigurationModule> implements CommonProgramRunConfigurationParameters
{
	private String programParameters;
	private String workingDir = "";
	private Map<String, String> envs = new LinkedHashMap<String, String>();
	private boolean passParentEnvs = true;

	public RustConfiguration(String name, Project project, RustConfigurationType configurationType)
	{
		super(name, new RunConfigurationModule(project), configurationType.getConfigurationFactories()[0]);
	}

	@Override
	public Collection<Module> getValidModules()
	{
		Module[] modules = ModuleManager.getInstance(getProject()).getModules();
		return Arrays.asList(modules);
	}

	@Override
	protected ModuleBasedConfiguration createInstance()
	{
		return new RustConfiguration(getName(), getProject(), RustConfigurationType.getInstance());
	}

	@NotNull
	@Override
	public SettingsEditor<? extends RunConfiguration> getConfigurationEditor()
	{
		SettingsEditorGroup<RustConfiguration> group = new SettingsEditorGroup<RustConfiguration>();
		group.addEditor(RustBundle.message("settings.editor.configuration.tab.title"), new RustRunSettingsEditor());
		return group;
	}

	@Nullable
	@Override
	public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException
	{
		Project project = getProject();

		if(Strings.isNullOrEmpty(this.workingDir))
		{
			this.workingDir = project.getBaseDir().getCanonicalPath();
		}

		CommandLineState state = new RustRunProfileState(env);

		state.setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(project));
		state.addConsoleFilters(new RustConsoleFilter(project, project.getBasePath()));
		return state;
	}

	@Override
	public void readExternal(final Element element) throws InvalidDataException
	{
		PathMacroManager.getInstance(getProject()).expandPaths(element);
		super.readExternal(element);
		XmlSerializer.deserializeInto(this, element);
		readModule(element);
		EnvironmentVariablesComponent.readExternal(element, getEnvs());
	}

	@Override
	public void writeExternal(final Element element) throws WriteExternalException
	{
		super.writeExternal(element);
		XmlSerializer.serializeInto(this, element);
		writeModule(element);
		EnvironmentVariablesComponent.writeExternal(element, getEnvs());
		PathMacroManager.getInstance(getProject()).collapsePathsRecursively(element);
	}

	@Override
	public void setProgramParameters(@Nullable String value)
	{
		this.programParameters = value;
	}

	@Nullable
	@Override
	public String getProgramParameters()
	{
		return programParameters;
	}

	@Override
	public void setWorkingDirectory(@Nullable String value)
	{
		this.workingDir = value;
	}

	@Nullable
	@Override
	public String getWorkingDirectory()
	{
		return this.workingDir;
	}

	@Override
	public void setEnvs(@NotNull Map<String, String> envs)
	{
		this.envs.clear();
		this.envs.putAll(envs);
	}

	@NotNull
	@Override
	public Map<String, String> getEnvs()
	{
		return envs;
	}

	@Override
	public void setPassParentEnvs(boolean passParentEnvs)
	{
		this.passParentEnvs = passParentEnvs;
	}

	@Override
	public boolean isPassParentEnvs()
	{
		return passParentEnvs;
	}
}
