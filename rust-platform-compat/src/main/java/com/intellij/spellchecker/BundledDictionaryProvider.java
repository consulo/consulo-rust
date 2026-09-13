package com.intellij.spellchecker;
/** Supplies the classpath paths of spellchecker dictionaries bundled with a plugin. */
public interface BundledDictionaryProvider {
    String[] getBundledDictionaries();
}
