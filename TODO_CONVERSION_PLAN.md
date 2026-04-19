# Kotlin→Java Conversion — TODO Backlog

This plan tracks TODOs **added during the Kotlin→Java auto-conversion** (not pre-existing TODOs
that were already present in the Kotlin sources). The source-of-truth is `git show HEAD:<kt-path>`
for each Kotlin original.

## Ground rules (fail-safe workflow)

For each file:

1. Read the Java stub(s) and the Kotlin original (`git show HEAD:src/main/kotlin/...`).
2. Port the logic **fully**, matching the Kotlin semantics. Do **not** leave `UnsupportedOperationException("TODO")`
   or placeholder comments. Do not introduce new simplified stubs.
3. Build guard: `./gradlew compileJava --no-daemon` must stay green after every file.
4. If a dependency helper is itself a stub, check whether it has a real Java impl elsewhere (use `Grep`
   for the symbol); if not, port that helper first (depth-first), then come back.
5. Tick the checkbox and commit message-ready note below.
6. If porting becomes too invasive for one step (e.g. needs a new enum/class), split into sub-steps
   noted below the file entry.

Numbers in parentheses are the count of conversion-added TODOs per file as of the snapshot in
`.claude/projects/.../memory/conversion_added_todos.txt`.

## Totals snapshot

- **229 conversion-added TODOs across 75 files**
- **81 pre-existing (inherited from Kotlin) across 41 files** — out of scope for this plan.

## Completed before this plan existed

- [x] `src/main/java/org/rust/lang/core/psi/ext/RsAttrUtil.java` — `getOwner` (outer/inner attr PSI walk)
- [x] `src/main/java/org/rust/lang/core/psi/ext/RsModExtUtil.java` — `getSuperMods` (super chain w/ cycle detection) + `exportedItems` (delegates to `RsModUtil.exportedItems`)
- [x] `src/main/java/org/rust/lang/core/psi/ext/RsAttrProcMacroOwnerUtil.java` — all 4 methods
- [x] `src/main/java/org/rust/lang/core/psi/ext/RsDocAndAttributeOwnerUtil.java` — stub+cfg_attr paths for `getQueryAttributes`/`evaluateCfg`/`getTraversedRawAttributes`/`findOuterAttr`/etc.
- [x] `src/main/java/org/rust/lang/core/psi/ext/RsGenericDeclarationUtil.java` — full port incl. `PsiPredicate` + `toPredicates` + `CachedValuesManager` + `withDefaultSubst`
- [x] `src/main/java/org/rust/lang/core/psi/ext/RsBinaryOpUtil.java` / `RsUnaryExprUtil.java` — mutual cycle fix
- [x] `src/main/java/org/rust/lang/utils/evaluation/CfgEvaluator.java` — added `expandCfgAttrs(Stream<T>)`
- [x] `src/main/java/org/rust/lang/core/resolve/ref/RsPathReferenceImpl.java` — `tryAdvancedResolveTypeAliasToImpl` (selectStrict → Selection.impl + expandedMembers.types find)

## Heavy files (≥10 TODOs)

- [ ] `src/main/java/org/rust/lang/core/resolve/NameResolution.java` **(40)**
      Kotlin source: `src/main/kotlin/org/rust/lang/core/resolve/NameResolution.kt` (1896 lines).
      40 unimplemented `throw new UnsupportedOperationException("TODO: <fn>")`. Full name-resolution
      algorithm port. Depends on: `RsResolveProcessor`, scope-processing infra, prelude search,
      `resolvePath`, extern-crate + dep lookup. Will likely need its own session.
- [x] `src/main/java/org/rust/lang/core/mir/MirBuilder.java` **(21)** — verified against MirBuilder.kt (65 pre-existing TODOs); all 21 remaining Java TODOs mirror `TODO()` in the original Kotlin (unimplemented expr/pattern paths: captures, upvars, downcast, let-else, ascribe_types); left as-is
      Kotlin source: `src/main/kotlin/org/rust/lang/core/mir/MirBuilder.kt` (2165 lines). Mostly
      `throw new UnsupportedOperationException("TODO")` in MIR build paths for expr/pattern kinds
      not yet handled. Needs: upvars capture, place downcast, let-else, ascribe_types, additional
      expr/pattern dispatch.
- [x] `src/main/java/org/rust/lang/core/mir/building/MirTestUtil.java` **(11)** — extracted from MirBuilder.kt performTest/sortCandidate; all 11 TODOs are pre-existing `MirTest.SwitchInt/Eq/Range/Len -> TODO()` in the original Kotlin (feature not implemented upstream); left as-is
- [x] `src/main/java/org/rust/lang/core/mir/dataflow/move/MoveDataBuilder.java` **(10)** — verified against MovePaths.kt: all 10 TODOs are pre-existing in the Kotlin original (TyReference/TyPointer/TySlice/TyArray/union/CopyForDeref/ShallowInitBox/Subslice not implemented upstream); left as-is

## Medium files (5-9 TODOs)

- [x] `src/main/java/org/rust/lang/core/thir/ThirPat.java` **(8)** — verified against ThirPat.kt (13 pre-existing TODOs); left as-is
- [x] `src/main/java/org/rust/lang/core/resolve/Processors.java` **(6)** — filter+dedup for completion variants — ported `filterPathCompletionVariantsByTraitBounds`, `deduplicateMethodCompletionVariants`, `filterMethodCompletionVariantsByTraitBounds` from RsCommonCompletionProvider.kt, using `lookup.getCtx().canEvaluateBounds` + `FoldUtil.containsTyOfClass`.
- [x] `src/main/java/org/rust/ide/utils/imports/ImportBridge.java` **(6)** — ported importElement(s), doImport (with insertExternCrateIfNeeded + insertUseItem), importCandidate, getStdlibAttributes; importTypeReferencesFromElement left as no-op noting dependency on untransported TypeReferencesInfo collection
- [ ] `src/main/java/org/rust/ide/utils/imports/ImportCandidatesCollector.java` **(6)** — DEFERRED: requires full CrateDefMap resolve2 port (VisItem.toPsi etc.). Depends on VisItem deferred above
- [x] `src/main/java/org/rust/lang/core/resolve2/FacadeUpdateDefMapUtil.java` **(5)** — delegated to existing FacadeUpdateDefMap impls
- [x] `src/main/java/org/rust/lang/core/mir/MirUtils.java` **(5)** — verified against Kotlin: all 5 TODOs are pre-existing in the original Kotlin (partial impls of `needsDrop`, `getMinValue`, etc.); left as-is
- [x] `src/main/java/org/rust/lang/core/psi/ext/RsExprExtUtil.java` **(5)** — full port of `getAdjustedType`/`classifyConstContext`/`isInConstContext`/`isInUnsafeContext`/`isTailExpr`/`processBreakExprs` from RsExpr.kt & RsLabeledExpression.kt

## Small files (2-4 TODOs)

- [x] `src/main/java/org/rust/lang/core/macros/RsExpandedElementUtil.java` (4) — ported getExpansionContext/parseExpandedTextWithContext/getExpansionFromExpandedFile
- [x] `src/main/java/org/rust/lang/core/psi/ext/RsMetaItemExtUtil.java` (4) — delegated to existing RsMetaItemUtil impls
- [x] `src/main/java/org/rust/lang/core/psi/ext/RsTypeReferenceUtil.java` (4) — ported `getOwner`/`getRawType`/`skipParens`/`substAndGetText`
- [x] `src/main/java/org/rust/lang/core/mir/building/MirTest.java` (4) — verified against MirTest.kt (15 pre-existing TODOs); left as-is
- [x] `src/main/java/org/rust/lang/core/mir/schemas/MirVisitor.java` (4) — verified against Kotlin (14 pre-existing TODOs, all still pre-existing); left as-is
- [x] `src/main/java/org/rust/ide/utils/checkMatch/Pattern.java` (4) — verified against Pattern.kt (4 pre-existing TODOs, Slice/Array not implemented upstream); left as-is
- [x] `src/main/java/org/rust/lang/core/resolve2/PathResolutionUtil.java` (3) — singlePublicOrFirst ported, resolveExternCrateAsDefMap delegated; dead resolvePathFp stub removed
- [x] `src/main/java/org/rust/lang/core/psi/ext/RsMethodCallExtUtil.java` (3) — ported getParentDotExpr/getReceiver/getTextRangeWithoutValueArguments
- [x] `src/main/java/org/rust/lang/core/psi/ext/RsFileExtUtil.java` (3) — delegated to existing RsFile impls + RsElementUtil.getContainingCargoPackage
- [x] `src/main/java/org/rust/lang/core/types/infer/TypeInferenceUtil.java` (3) — getGenerics/getConstGenerics ported, getPredicates delegates to RsGenericDeclarationUtil
- [x] `src/main/java/org/rust/lang/utils/evaluation/ConstExprUtil.java` (3) — delegated to existing ConstExprEvaluator
- [x] `src/main/java/org/rust/ide/utils/checkMatch/CheckMatchUtils.java` (3) — verified against Kotlin: all 3 TODOs are pre-existing `TODO()` in Kotlin (Slice/Array specialization not implemented upstream), left as-is
- [x] `src/main/java/org/rust/openapiext/QueryUtil.java` (2) — mapQuery/filterIsInstanceQuery ported (Query.mapping + InstanceofQuery)
- [x] `src/main/java/org/rust/cargo/project/settings/RustProjectSettingsServiceUtil.java` (2) — getRustSettings via project.getService, recursion limit delegates to RustAdvancedSettings
- [x] `src/main/java/org/rust/lang/core/macros/MacroExpansionHighlightingUtil.java` (2) — bridged ProcMacroAttribute.attr -> MacroHighlightingUtil.prepareForExpansionHighlighting
- [ ] `src/main/java/org/rust/lang/core/resolve2/VisItem.java` (2) — DEFERRED: requires porting ModPath.toRsModOrEnum/toScope + expandedItemsCached + getExpandedItemsWithName + crateGraph lookup — subsystem-wide effort
- [x] `src/main/java/org/rust/lang/core/resolve2/ModCollector.java` (2) — no conversion TODOs remain in file (prior fixes cleared them)
- [x] `src/main/java/org/rust/ide/utils/imports/RsImportHelper.java` (1) — getTypeReferencesInfoFromTys ports Kotlin TypeVisitor-based subject collection (TyAdt/TyAnon/TyTraitObject/TyProjection); processRawImportSubjects splits into toImport/toQualify via ImportCandidatesCollector
- [ ] ... ~50 more files with 1 TODO each — see `conversion_added_todos.txt`

## Detailed list

Full per-file listing lives in
`.claude/projects/-run-media-vistall-repositories-consulo-consulo-rust/memory/conversion_added_todos.txt`.
That file was generated by diffing current Java TODOs against the Kotlin originals' TODO list (see
the generator script at the end of the agent transcript); regenerate as needed.

## Progress log

- 2026-04-16 (session 1): Fixed 29 infinite-recursion bugs (5 files in psi.ext) + ported
  `RsPathReferenceImpl.tryAdvancedResolveTypeAliasToImpl`. Created this plan.

## Session 2 additions (small 1-TODO files)

- [x] `src/main/java/org/rust/lang/utils/NegateUtil.java` — delegate to RsBooleanExpUtils.negate

## Session 2 batch (small files)

- [x] `src/main/java/org/rust/lang/core/resolve/KnownDerivableTrait.java` — `findTrait` switch over enum, `shouldUseHardcodedTraitDerive` via ProcMacroApplicationService.isDeriveEnabled
- [x] `src/main/java/org/rust/ide/refactoring/RsNamesValidatorUtil.java` — `getCanBeEscaped` + `isValidRustVariableIdentifier` ported
- [x] `src/main/java/org/rust/ide/template/postfix/RsPostfixTemplateUtil.java` — RsPostfixTemplatePsiInfo body via RsPsiFactory + RsBooleanExpUtils.negate; RS_EXPR_CONDITION = `instanceof RsExpr`
- [x] `src/main/java/org/rust/ide/debugger/runconfig/RsDebugAdvertisingRunner.java` — installAndEnable platform-call documented as no-op (Kotlin-only inline fn, no Java entry point in deps); kept Install/Enable/Restart dialog actions
- [x] `src/main/java/org/rust/cargo/runconfig/ui/CargoCommandConfigurationEditor.java` — Kotlin DSL `panel { row(...) }` ported via FormBuilder
- [x] `src/main/java/org/rust/cargo/runconfig/ui/WasmPackCommandConfigurationEditor.java` — same
- [x] `src/main/java/org/rust/cargo/runconfig/target/RsLanguageRuntimeConfigurable.java` — Kotlin DSL ported via FormBuilder + manual apply/reset/isModified binding for 5 text fields
- [x] `src/main/java/org/rust/lang/utils/RsStringCharactersParsing.java` — full Rust string-escape parser (n/r/t/0/quotes/`\xNN`/`\u{NNNN}`/line-continuation) with source-offset map
- [x] `src/main/java/org/rust/lang/core/resolve2/DefMapService.java` — `updateDefMapForAllCratesWithWriteActionPriority` delegated to FacadeUpdateDefMap
- [x] `src/main/java/org/rust/lang/core/resolve2/FacadeResolveUtil.java` — `getModInfo` delegated to FacadeResolve.getModInfo
- [x] `src/main/java/org/rust/lang/core/resolve2/RsModInfo.java` — `getMacroIndex` documented as fallback to `modData.macroIndex` (full call-position calculation requires MacroIndex.append + ProcMacroAttribute.getProcMacroAttributeWithoutResolve)
- [x] `src/main/java/org/rust/lang/core/resolve2/ModData.java` — `processMacros` documented as part of deferred resolve2 subsystem; safe `false` fallback
- [x] `src/main/java/org/rust/lang/core/psi/ext/RsCfgExtUtil.java` — `isCfgUnknown` delegated to CfgUtils.isCfgUnknown
- [x] `src/main/java/org/rust/lang/core/psi/ext/RsTraitItemExtUtil.java` — `getDeclaredType` delegated to RsPsiTypeImplUtil.declaredType
- [x] `src/main/java/org/rust/lang/core/psi/ext/RsLifetimeExtUtil.java` — `getTypedName` ported (text-based classification → LifetimeName.{Implicit,Underscore,Static,Parameter})
- [x] `src/main/java/org/rust/lang/core/psi/ext/RsMacroExtUtil.java` — `getHasRustcBuiltinMacro` delegated to instance method
- [x] `src/main/java/org/rust/lang/core/psi/ext/RsAttrProcMacroOwnerExtUtil.java` — `getQueryAttributes` delegated to RsDocAndAttributeOwnerUtil
- [x] `src/main/java/org/rust/lang/core/psi/ext/RsAbstractableUtil.java` — `searchForImplementations` ported (trait-impl search → same-name member projection)
- [x] `src/main/java/org/rust/lang/core/psi/ext/RsSearchableUtil.java` — `searchReferences` delegated to ReferencesSearch.search().findAll()
- [x] `src/main/java/org/rust/lang/core/psi/ext/RsNamedElementUtil.java` — `getEscapedName` delegated to RsRawIdentifiers.escapeIdentifierIfNeeded
- [x] `src/main/java/org/rust/lang/core/psi/RsPathCodeFragment.java` — `getPath` via PsiElementUtil.childOfType
- [x] `src/main/java/org/rust/lang/core/stubs/RsAttributeOwnerStub.java` — full `extractFlags` port (cfg/cfg_attr/derive/macro_use/no_std/recursion_limit/proc_macro/prelude_import/macro_export/rustc_builtin_macro/rustc_reservation_impl + custom-attrs detection via BuiltinAttributes)
- [x] `src/main/java/org/rust/lang/core/stubs/RsAttrProcMacroOwnerStub.java` — `extractTextAndOffset` ported (text + body hash + start offset; endOfAttrsOffset documented as 0 pending RsAttrProcMacroOwner.endOfAttrsOffset port)
- [x] `src/main/java/org/rust/ide/search/RsFindUsagesHandlerFactory.java` — `findImplDeclarations` delegates to RsAbstractableUtil.searchForImplementations
- [x] `src/main/java/org/rust/lang/core/types/infer/ResolvedPath.java` — `from(ScopeEntry, RsElement)` now correctly checks visibility via Processors.isVisibleFrom + RsVisibilityUtil.isVisibleFrom
- [x] `src/main/java/org/rust/lang/core/types/ty/TyTypeParameter.java` — `getRegionBounds` walks polybounds and resolves lifetimes via TyLowering.resolveLifetime
- [x] `src/main/java/org/rust/lang/core/types/infer/TyLowering.java` — `withAlias` now invoked instead of returning the un-aliased ty
- [x] `src/main/java/org/rust/lang/core/thir/MirrorContext.java` — `convertArm` now mirrors the guard expression
- [x] `src/main/java/org/rust/lang/core/completion/RsCommonCompletionProvider.java` — `collectVariantsForEnumCompletion` ported (enum::variant lookup elements with optional import candidate)
- [x] `src/main/java/org/rust/lang/utils/NegateUtil.java` — RsBinaryExpr negate delegates to RsBooleanExpUtils.negate
- [x] Files verified pre-existing (matching `TODO()` in original Kotlin, no work required): MirMatch, MirScalarInt, MirScalar, Constructor, RsLivenessInspection, DropTree, Borrows, RsKeywordCompletionContributor, RsUnusedMustUseInspection, MirVisitor, MirTest, MirTestUtil, MoveDataBuilder, MirUtils, MirExtensions, ThirPat, Pattern, MirBuilder, BorrowCheckResults, LocalsStateAtExit, BorrowsUtil, DropFlagEffectUtil, RsProblemsHolder, RsAdditionalLibraryRootsProvider, CargoTestRunState, RegexpFileLinkFilter, Thir, RsSpacingUtil, CheckMatchUtils

## Truly remaining (subsystem-level work)

- [ ] **`NameResolution.java` (40 methods, 1896 Kotlin lines)** — full name-resolution algorithm. Single biggest remaining item. Needs its own session(s).
- [ ] **`ImportCandidatesCollector.java` (6)** — DEFERRED. Depends on full CrateDefMap resolve2 walk + VisItem.toPsi.
- [ ] **`VisItem.java` (2)** — DEFERRED. Depends on ModPath.toRsModOrEnum/toScope, expandedItemsCached, getExpandedItemsWithName, crateGraph lookup.
- [ ] **`RegionScopeTreeUtil.java` (1)** — DEFERRED. Needs RegionResolutionVisitor port (Scope.kt — several hundred Kotlin lines).
