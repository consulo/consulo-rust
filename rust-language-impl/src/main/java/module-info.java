/**
 * The Rust language implementation: lexer, parser, PSI implementations, stubs, name
 * resolution, type inference and macro expansion.
 */
open module consulo.rust.language.impl {
    requires transitive consulo.rust.base;
    requires transitive consulo.rust.cargo.api;
    requires transitive consulo.rust.platform.compat;
    requires consulo.language.api;
    requires consulo.language.impl;
    requires consulo.virtual.file.system.api;
    requires consulo.ui.api;
    requires consulo.ui.ex.api;
    requires consulo.localize.api;
    requires consulo.annotation;
    requires com.intellij.regexp;
    requires jakarta.annotation;
    requires transitive consulo.rust.language.api;

    exports org.rust.lang.core;
    exports org.rust.lang.core.completion;
    exports org.rust.lang.core.crate;
    exports org.rust.lang.core.crate.impl;
    exports org.rust.lang.core.dfa;
    exports org.rust.lang.core.dfa.borrowck;
    exports org.rust.lang.core.dfa.borrowck.gatherLoans;
    exports org.rust.lang.core.dfa.liveness;
    exports org.rust.lang.core.format;
    exports org.rust.lang.core.imports;
    exports org.rust.lang.core.injected;
    exports org.rust.lang.core.lexer;
    exports org.rust.lang.core.macros;
    exports org.rust.lang.core.macros.builtin;
    exports org.rust.lang.core.macros.decl;
    exports org.rust.lang.core.macros.errors;
    exports org.rust.lang.core.macros.proc;
    exports org.rust.lang.core.macros.tt;
    exports org.rust.lang.core.match;
    exports org.rust.lang.core.mir;
    exports org.rust.lang.core.mir.borrowck;
    exports org.rust.lang.core.mir.building;
    exports org.rust.lang.core.mir.dataflow.framework;
    exports org.rust.lang.core.mir.dataflow.impls;
    exports org.rust.lang.core.mir.dataflow.move;
    exports org.rust.lang.core.mir.schemas;
    exports org.rust.lang.core.mir.schemas.impls;
    exports org.rust.lang.core.mir.util;
    exports org.rust.lang.core.names;
    exports org.rust.lang.core.parser;
    exports org.rust.lang.core.presentation;
    exports org.rust.lang.core.psi;
    exports org.rust.lang.core.psi.ext;
    exports org.rust.lang.core.psi.ext.impl;
    exports org.rust.lang.core.psi.impl;
    exports org.rust.lang.core.resolve;
    exports org.rust.lang.core.resolve.indexes;
    exports org.rust.lang.core.resolve.ref;
    exports org.rust.lang.core.resolve2;
    exports org.rust.lang.core.resolve2.actions;
    exports org.rust.lang.core.resolve2.util;
    exports org.rust.lang.core.search;
    exports org.rust.lang.core.stubs;
    exports org.rust.lang.core.stubs.common;
    exports org.rust.lang.core.stubs.index;
    exports org.rust.lang.core.thir;
    exports org.rust.lang.core.types;
    exports org.rust.lang.core.types.consts;
    exports org.rust.lang.core.types.infer;
    exports org.rust.lang.core.types.regions;
    exports org.rust.lang.core.types.ty;
    exports org.rust.lang.doc;
    exports org.rust.lang.doc.psi;
    exports org.rust.lang.doc.psi.ext;
    exports org.rust.lang.doc.psi.impl;
    exports org.rust.lang.utils;
    exports org.rust.lang.utils.evaluation;
    exports org.rust.lang.utils.snapshot;
}
