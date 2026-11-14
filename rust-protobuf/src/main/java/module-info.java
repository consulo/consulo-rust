/**
 * Rust support that needs the Protocol Buffers plugin: the code prost and tonic generate from {@code .proto} files.
 * <p>
 * {@code META-INF/plugin-requires.xml} names {@code consulo.google.protobuf}, so this module is loaded only when that
 * plugin is installed.
 */
open module consulo.rust.protobuf {
    requires consulo.rust.language.impl;
    requires consulo.rust.cargo.api;

    requires consulo.google.protobuf.api;

    requires consulo.annotation;
    requires consulo.application.api;
    requires consulo.component.api;
    requires consulo.language.api;
    requires consulo.project.api;
    requires consulo.virtual.file.system.api;

    exports org.rust.protobuf;
}
