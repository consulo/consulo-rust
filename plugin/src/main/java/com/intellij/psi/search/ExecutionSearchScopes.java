package com.intellij.psi.search;
import consulo.project.Project;
import consulo.language.psi.scope.GlobalSearchScope;
/** IntelliJ-compat stub. */
public final class ExecutionSearchScopes {
    public static GlobalSearchScope executionScope(Project project, Object runProfile) { return GlobalSearchScope.allScope(project); }
}
