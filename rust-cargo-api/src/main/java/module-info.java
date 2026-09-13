/**
 * The cargo contract the language layer depends on: workspace and project model, cfg
 * options, settings and toolchain data.
 */
open module consulo.rust.cargo.api {
    requires transitive consulo.rust.base;
    requires transitive consulo.rust.platform.compat;
    requires consulo.language.api;
    requires consulo.language.impl;
    requires consulo.virtual.file.system.api;
    requires consulo.ui.api;
    requires consulo.ui.ex.api;
    requires consulo.localize.api;
    requires consulo.annotation;
    requires jakarta.annotation;

    exports org.rust.cargo.api;
    exports org.rust.cargo.api.model;
    exports org.rust.cargo.api.settings;
    exports org.rust.cargo.api.toolchain;
    exports org.rust.cargo.api.util;
    exports org.rust.cargo.api.workspace;
}
