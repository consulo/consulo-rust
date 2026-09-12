package com.intellij.codeInsight.template;
import com.intellij.codeInsight.template.impl.TemplateImpl;
import org.jdom.Element;
public final class TemplateSettings {
    private TemplateSettings() {}
    public static final String TEMPLATE = "template";
    public static TemplateImpl readTemplateFromElement(String groupName, Element element, ClassLoader classLoader) {
        return null;
    }
}
