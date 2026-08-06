package com.intellij.codeInsight.hints;

import consulo.language.Language;
import java.util.Collections;
import java.util.List;

/** IntelliJ-compat stub — inlay hint info. */
public abstract class HintInfo {
    public static class MethodInfo extends HintInfo {
        public final String fullyQualifiedName;
        public final List<String> paramNames;
        public final Language language;
        public MethodInfo(String fullyQualifiedName, List<String> paramNames, Language language) {
            this.fullyQualifiedName = fullyQualifiedName;
            this.paramNames = paramNames;
            this.language = language;
        }
        public MethodInfo(String fullyQualifiedName, List<String> paramNames) {
            this(fullyQualifiedName, paramNames, null);
        }
    }
    public static class OptionInfo extends HintInfo {
        public final Option option;
        public OptionInfo(Option option) { this.option = option; }
    }
    public static class Option {
        public final String name;
        public Option(String name) { this.name = name; }
    }
}
