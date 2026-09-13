package com.intellij.codeInsight.template.impl;
public class TemplateImpl {
    private String template;
    public TemplateImpl(String key, String template, String group) { this.template = template; }
    public String getString() { return template; }
}
