package com.intellij.navigation;
import consulo.ide.navigation.ChooseByNameContributorEx;
public interface GotoClassContributor extends ChooseByNameContributorEx {
    String getQualifiedName(consulo.navigation.NavigationItem item);
    default String getQualifiedNameSeparator() { return "."; }
}
