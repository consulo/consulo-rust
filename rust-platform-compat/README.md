# rust-platform-compat

**Legacy. Unsupported. Do not build on this.**

Everything in this module carries an IntelliJ package name — `com.intellij.*`,
`com.jetbrains.*`. None of it is Consulo API. These classes exist for one reason:
the Rust plugin was machine-translated from [intellij-rust], and the translated
sources still reference IntelliJ types that Consulo either names differently,
models differently, or does not have at all.

Rather than rewrite 2000+ files at once, those types were reimplemented here
against real Consulo APIs, keeping their original names so the ported code
compiles. Each one is a stop-gap.

[intellij-rust]: https://github.com/intellij-rust/intellij-rust

## What this means in practice

* **An IntelliJ class name here is not a promise.** The class is a local
  reimplementation. It may be a thin delegate over the Consulo equivalent, a
  partial implementation covering only what the plugin happens to call, or —
  in a few cases — a stub whose methods do nothing useful.
* **Consulo does not support any of it.** Nothing here tracks the IntelliJ
  Platform. If IntelliJ changes a signature, this module does not care and
  should not be updated to match.
* **Behaviour may silently differ.** A method that looks like its IntelliJ
  namesake can have different threading rules, different nullability, or no
  effect at all. When something behaves oddly, check here first.

## Rules

1. **Do not add classes to this module.** Write against the Consulo API instead.
   The only reason to touch it is to remove something.
2. **Prefer the Consulo API at every call site.** When you are editing ported
   code that uses a shim, replace the usage if the Consulo equivalent is known.
3. **When the last caller of a shim is gone, delete the shim.** This module
   should shrink monotonically. It is currently 167 files.
4. **If Consulo genuinely lacks a capability, say so at the point of failure** —
   a comment where the feature degrades, not a silent no-op — and raise it
   against Consulo rather than deepening the shim.

## Layout

Packages mirror IntelliJ's, so a ported file's `import com.intellij.…` resolves
without edits. The module is `open` and exports every package it contains,
because the ported sources import from all of them.

Nothing here depends on Rust — no source file imports `org.rust.*` or
`consulo.rust.*` (only `module-info.java` names the module itself), and it must
stay that way. This module is a leaf of the build graph.

## Goal

Delete this module. Every class removed is one less place where the plugin
pretends to be running on IntelliJ.
