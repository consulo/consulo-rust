/**
 * The Rust language contract: the {@code Language}, the file type and the naming constants.
 * <p>
 * Depended on by everything that needs to say "this is Rust" without pulling in the parser, the
 * PSI implementations or the resolve engine.
 */
open module consulo.rust.language.api {
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
    requires jakarta.annotation;

    exports org.rust.lang;
}
