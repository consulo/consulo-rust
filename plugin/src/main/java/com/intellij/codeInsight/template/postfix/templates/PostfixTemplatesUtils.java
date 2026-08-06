package com.intellij.codeInsight.template.postfix.templates;

import consulo.language.editor.postfixTemplate.PostfixTemplate;
import org.jdom.Element;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/** IntelliJ-compat stub. */
public final class PostfixTemplatesUtils {
    private PostfixTemplatesUtils() {}
    public static final String TOPMOST_ATTR = "topmost";

    public static List<?> readExternalConditions(Element parent, Function<Element, ?> reader) {
        return Collections.emptyList();
    }
    public static void writeExternalTemplate(PostfixTemplate template, Element parent) {}
    public static void writeExternalConditions(Element parent, Object conditions) {}
}
