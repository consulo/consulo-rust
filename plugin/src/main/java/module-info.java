/**
 * Consulo Rust plugin module descriptor.
 * Open module — all packages are opened for reflection so Consulo's component
 * container / XML serializer / annotation scanner can access @ExtensionImpl,
 * @ServiceImpl, @State, etc. without per-package opens directives.
 */
open module consulo.rust {
    requires transitive consulo.rust.cargo.api;
    requires java.desktop;
    requires java.xml;

    // External libraries
    requires com.google.gson;
    requires org.eclipse.jgit;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.module.paramnames;
    requires com.fasterxml.jackson.dataformat.toml;
    requires consulo.markdown.engine;
    requires javasemver;
    requires org.apache.commons.lang3;
    requires org.jdom;

    // Consulo core
    requires consulo.annotation;
    requires consulo.container.api;
    requires consulo.disposer.api;
    requires consulo.logging.api;
    requires consulo.localize.api;
    requires consulo.proxy;
    requires consulo.base.localize.library;
    requires consulo.base.icon.library;
    requires consulo.platform.api;

    // Util
    requires consulo.util.collection;
    requires consulo.util.concurrent;
    requires consulo.util.dataholder;
    requires consulo.util.io;
    requires consulo.util.jdom;
    requires consulo.util.lang;
    requires consulo.util.xml.fast.reader;
    requires consulo.util.xml.serializer;

    // UI
    requires consulo.ui.api;
    requires consulo.ui.ex.api;
    requires consulo.ui.ex.awt.api;
    requires consulo.datacontext.api;

    // Application
    requires consulo.application.api;
    requires consulo.application.content.api;
    requires consulo.application.ui.api;
    requires consulo.application.impl;
    requires consulo.component.api;
    requires consulo.component.store.api;
    requires consulo.configurable.api;
    requires consulo.index.io;
    requires consulo.color.scheme.api;

    // Document / virtual file system
    requires consulo.document.api;
    requires consulo.document.impl;
    requires consulo.virtual.file.system.api;
    requires consulo.virtual.file.watcher.api;

    // Project / module
    requires consulo.project.api;
    requires consulo.project.content.api;
    requires consulo.project.ui.api;
    requires consulo.module.api;
    requires consulo.module.content.api;
    requires consulo.module.ui.api;
    requires consulo.module.creation.api;

    // File chooser / editor / template
    requires consulo.file.chooser.api;
    requires consulo.file.editor.api;
    requires consulo.file.template.api;

    // Code editor
    requires consulo.code.editor.api;

    // Process / execution
    requires consulo.process.api;
    requires consulo.execution.api;
    requires consulo.execution.debug.api;
    requires consulo.execution.test.api;
    requires consulo.execution.test.sm.api;
    requires consulo.execution.impl;

    // Language
    requires consulo.language.api;
    requires consulo.language.impl;
    requires consulo.language.editor.api;
    requires consulo.language.editor.impl;
    requires consulo.language.editor.refactoring.api;
    requires consulo.language.editor.ui.api;
    requires consulo.language.code.style.api;
    requires consulo.language.code.style.ui.api;
    requires consulo.language.spellchecker.api;
    requires consulo.language.copyright.api;
    requires consulo.language.inject.advanced.api;
    requires consulo.execution.coverage.api;
    requires consulo.language.duplicate.analysis.api;

    // Higher-level services
    requires consulo.build.ui.api;
    requires consulo.compiler.api;
    requires consulo.find.api;
    requires consulo.navigation.api;
    requires consulo.usage.api;
    requires consulo.undo.redo.api;
    requires consulo.external.service.api;
    requires consulo.external.system.api;
    requires consulo.version.control.system.api;
    requires consulo.web.browser.api;
    requires consulo.http.api;

    // IDE (last because it pulls many transitively)
    requires consulo.ide.api;
    requires consulo.ide.impl;

    requires consulo.rust.platform.compat;
    requires consulo.rust.base;
    requires consulo.rust.language.api;
    requires consulo.rust.language.impl;

    requires org.toml.lang;
    requires com.intellij.regexp;
}
