# Architecture

This is a port of [intellij-rust] to the [Consulo] platform. The language
support — lexer, parser, PSI, name resolution, type inference — is a Java
translation of the original Kotlin. Everything that touches the IDE around it —
registration, the project model, SDKs, building, running — is written against
Consulo's APIs and does not resemble the original.

Read this document as two halves. [Language support](#language-support) is
essentially upstream's design and upstream's documentation still applies to it.
[Platform integration](#platform-integration) is where this project differs, and
is the part worth reading before changing anything.

[intellij-rust]: https://github.com/intellij-rust/intellij-rust
[Consulo]: https://github.com/consulo/consulo

## Repository layout

```
plugin/          the only Maven module; everything that ships
  src/main/java/org/rust/...      ported language + IDE support
  src/main/java/consulo/rust/...  Consulo-specific integration
  src/main/resources/META-INF/plugin.xml
legacy/          upstream modules not ported (clion, debugger, profiler, ...)
exampleProject/  template sources for the new-project wizard
```

`legacy/` is kept for reference only. Nothing in it is built, and none of it is
reachable from `plugin/`.

## Build

Maven, not Gradle. The root `pom.xml` is a parent with a single module,
`plugin`. There is no Gradle anywhere in the project and nothing should
reintroduce it.

The `consulo-maven-plugin` runs four generators at `generate-sources`:

| goal | input | output |
| --- | --- | --- |
| `generate-lexers` | `org/rust/lang/core/lexer/RustLexer.flex` | JFlex lexer |
| `generate-parsers` | `org/rust/lang/core/parser/RustParser.bnf` | Grammar-Kit parser + PSI interfaces |
| `generate-icon` | icon resources | icon accessor class |
| `generate-localize` | `LOCALIZE-LIB/**` | localization accessors |

Generated sources land in `plugin/target/generated-sources/parsers` and are **not**
checked in. When a PSI interface looks wrong, regenerate before reading it —
and read it there, not anywhere else.

`module-info.java` declares the JPMS module `consulo.rust`. It is an `open`
module so Consulo's component container can reflect over every package. Adding a
dependency on a new Consulo API means adding a `requires` line there as well as
to `plugin/pom.xml`; a missing `requires` fails at runtime, not at compile time.

## Registration

Consulo discovers everything through annotations. There is no XML registration
beyond the plugin descriptor itself, and **IntelliJ's `@Service` annotation is
inert here** — a class carrying it is simply never instantiated.

| annotation | purpose | count |
| --- | --- | --- |
| `@ExtensionImpl` | an extension point implementation | ~289 |
| `@ServiceAPI` / `@ServiceImpl` | service interface / implementation | ~33 each |
| `@ActionImpl` | an action, including its menu placement | ~30 |
| `@TopicAPI` / `@TopicImpl` | message-bus topic / subscriber | 7 each |
| `@Inject` | constructor injection (`jakarta.inject`) | ~30 |

`@ActionImpl` carries the action's parents and anchors in the annotation, so
there is no `<actions>` section to keep in sync.

## Platform integration

### Toolchain as an SDK

A Rust toolchain is a Consulo *bundle* (SDK), described by
`consulo.rust.bundle.RustBundleType`. The bundle home is the **toolchain root**
— a rustup toolchain directory — never a `bin` directory, because Consulo
watches the SDK home and watching a directory of executables is wrong.

`RustBundleType` detects candidates from `$RUSTUP_HOME/toolchains`,
`~/.rustup/toolchains`, and the sysroot of a `rustc` found on `PATH`. A home is
valid when it has `bin/rustc`, `bin/cargo` and `lib/rustlib/src/rust`. Only the
standard-library sources are attached as a root — `lib/rustlib/src/rust/library`
as `SourcesOrderRootType` — which is what makes `String` and the rest of `std`
resolve and navigate.

### Module extension

`consulo.rust.module.extension.RustModuleExtension` marks a module as Rust and
binds it to a bundle. **This is the single source of truth for which toolchain to
use.** Building, running, `cargo` invocations, the external linter and the
crates.io index refresh all resolve their toolchain through
`RustModuleExtension.findToolchain(module)`. Nothing should search `PATH` for a
Rust installation on its own.

### Building

Building goes through Consulo's regular compiler API, not through a private
build session.

`consulo.rust.compiler.CargoCompilerRunner` implements
`consulo.compiler.CompilerRunner`. `CompileDriver` selects it by asking each
registered runner `checkAvailable`, and it answers yes when the project has a
module with a `RustModuleExtension`. It then builds a `cargo` command line,
runs it with `--message-format=json`, and translates each diagnostic into a
`BuildProgress` event. The platform owns the progress UI, cancellation and the
build tool window.

Two things about `CompilerRunner` are easy to get wrong:

* the `DataContext` handed to `checkAvailable` is a view over the **compile
  scope's user data only**. `Project.KEY` is not in it. Inject the project
  instead, and gate on `ModuleExtensionHelper.hasModuleExtension`.
* `CompilerManager.make` asserts it is called on the UI thread.

A run configuration only gets a make-before-run task if it implements
`RunProfileWithCompileBeforeLaunchOption`; `RsCommandConfiguration` does, and its
`getModules()` returns the Rust modules, which is what puts the build on the
module extension's toolchain. The plugin defines **no** `BeforeRunTaskProvider`
of its own — the platform's `CompileStepBeforeRun` covers every case, including
wasm-pack, whose different command line is produced inside the compiler runner.

### Project model

Unchanged from upstream in shape: a project holds `CargoProject`s, each with one
`CargoWorkspace`, holding `Package`s, holding `Target`s. A target is what Rust
calls a crate.

```
              CargoProject
                    |
              CargoWorkspace
              /            \
         Package          Package
          /    \             |
     Target   Target      Target
    (main.rs) (lib.rs)   (lib.rs)
```

`CargoProject` is persisted and always present; `CargoWorkspace` comes from
`cargo metadata` and is null while a project is invalid or still syncing.
Dependencies in `Cargo.toml` are packages with one library target; `extern crate
foo` names a target. Target names appear in Rust code with `-` replaced by `_`
— use `normName`.

Workspace changes are published on `CargoProjectsService.CARGO_PROJECTS_TOPIC`.
Anything cached against the workspace must invalidate on it; see
`RsCargoStructureInvalidator`.

## Language support

The parts below are upstream's design, translated. Upstream's documentation and
the [IntelliJ SDK docs][sdk-docs] still describe them accurately.

[sdk-docs]: https://plugins.jetbrains.com/docs/intellij/custom-language-support.html

### Packages

* `org.rust.lang` — lexer, parser, PSI, name resolution, type inference.
* `org.rust.cargo` — Cargo and rustup integration, project model, toolchain.
* `org.rust.ide` — everything the user sees: inspections, intentions,
  completion, navigation, refactoring.

### PSI

Grammar-Kit generates an interface per rule (`RsStructItem`) and an
implementation (`RsStructItemImpl`). Custom behaviour is added by having the
generated implementation extend a hand-written mixin (`RsStructItemImplMixin`),
or by a static helper class.

The Kotlin→Java conversion turned extension-function files into `XxxUtil`
classes of static methods. Where the logic was folded into a mixin instead, the
`Util` class may not exist at all — check the mixin first.

### Name resolution

Two mechanisms, and knowing which one answers a given question saves a lot of time.

**Def maps (`org.rust.lang.core.resolve2`, 58 files)** build a per-crate map of
what each module declares and imports, mirroring rustc's own resolution order.
`CrateDefMap` holds `ModData` per module; `ModCollector` walks stubs to collect
items; `DefCollector` resolves the imports between them. This is what answers
"what does this module contain", including `use` re-exports across crates.

**Lexical walk (`NameResolution.java`)** walks PSI outwards from the reference
through enclosing scopes, and consults the def map when it reaches a module.
This is what answers "what is in scope here".

A path like `Type::assoc_fn` is resolved by neither directly: the qualifier is
lowered to a `Ty` and the associated item is found through `ImplLookup`, which
searches the impl index. `Self::` and generic types need care — a generic type's
parameters become inference variables during resolution, and those must be
folded away before the result is cached (`FoldUtil.foldTyInferWithTyPlaceholder`).

Results are cached against modification trackers. Anything derived from the
workspace must depend on the Rust structure modification tracker, not on the PSI
tracker alone.

### Type inference

`org.rust.lang.core.types.infer`, modelled on rustc. Inference runs per
`RsInferenceContextOwner` (a function or constant body), walking it top-down to
build a map from expressions to types. Unknown types become variables in a
`UnificationTable`; trait obligations are collected and discharged through
`ImplLookup.select`.

Because inference is cached per function body, **never call
`ExtensionsUtil.getType()` from inside inference** — it re-enters the cached
inference of the same function and recurses without bound. Derive the type from
the declaration instead.

### Indexing and stubs

Stubs are a condensed AST holding what resolution needs and nothing more, stored
in a compact binary form so declarations can be listed without parsing. PSI
switches between stub-backed and AST-backed transparently, so an API that only
works on AST will silently force a reparse.

All other indexes are built on stubs. The index is only correct per-file: an
indexer may look at one file and nothing else.

**Resolution reads the impl index.** A lookup that runs before indexing finishes
sees no impls at all and will cache that empty answer, which is the usual cause
of a reference that is red until something invalidates it — see
`RsIndexReadyInvalidator`.

## Current state

Working: project sync, def-map construction, the SDK and module extension,
building through the compiler API, the external linter, and most navigation and
highlighting.

Known gaps, in the order they matter:

* **Macro expansion is a placeholder.** `DeclMacroExpander` and
  `ProcMacroExpander` do not expand. Anything generated by a derive or a
  function-like macro — `#[derive(Parser)]`, for instance — does not resolve.
* **Trait selection is partial.** `ImplLookup` assembles candidates from impls
  and caller bounds, but the builtin-bound, projection and object-type candidate
  families are not ported.
* **Method resolution is sensitive to index timing**, as described above.
* Several IntelliJ-only features have no Consulo equivalent and were dropped
  deliberately: trusted-project state, dynamic plugin unloading, the
  macro-expansion virtual file system's global index filter.

When the platform genuinely cannot do something, say so in a comment at the
point of failure rather than leaving a silent no-op.
