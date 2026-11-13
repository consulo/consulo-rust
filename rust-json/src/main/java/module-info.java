/**
 * Rust support that needs the JSON plugin.
 * <p>
 * {@code META-INF/plugin-requires.xml} names {@code consulo.json}, so this module is loaded only
 * when that plugin is installed.
 */
open module consulo.rust.json {
    requires consulo.rust.base;

    requires consulo.json.api;
    requires consulo.json.scheme.api;

    requires consulo.annotation;
    requires consulo.application.api;
    requires consulo.project.api;
    requires consulo.virtual.file.system.api;

    exports org.rust.toml.jsonSchema;
}
