package com.intellij.openapi.roots;
import consulo.project.Project;
import java.util.Collection;
import java.util.Collections;
/** IntelliJ-compat stub. Consulo uses Module Extensions to provide library roots. */
public abstract class AdditionalLibraryRootsProvider {
    public Collection<SyntheticLibrary> getAdditionalProjectLibraries(Project project) { return Collections.emptyList(); }
}
