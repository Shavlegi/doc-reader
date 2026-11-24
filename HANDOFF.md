# HANDOFF — Doc Reader v1 scaffold + reader

Repo: https://github.com/Shavlegi/doc-reader (public, main, EMPTY at build time).
Local code: `~/workspace/doc-reader/`. **NOT pushed** — parent reviews and pushes.

## Files created

```
README.md
HANDOFF.md (this file)
.gitignore
build.gradle.kts                  (root: AGP 8.5.2, Kotlin 2.0.21, serialization + compose plugins)
settings.gradle.kts               (rootProject.name = "DocReader", include(":app"))
gradle.properties                 (mirrors fake-gps-app)
gradlew / gradlew.bat             (copied from fake-gps-app)
gradle/wrapper/gradle-wrapper.jar + gradle-wrapper.properties (copied from fake-gps-app, Gradle 8.7)
.github/workflows/android.yml    (mirrors fake-gps build-apk.yml; artifact name doc-reader-debug-apk)
app/build.gradle.kts              (namespace com.shavlegi.docreader, minSdk 26, targetSdk 34, v1.0)
app/proguard-rules.pro            (stock template, minify OFF like the reference apps)
app/src/main/AndroidManifest.xml  (no INTERNET permission; single MainActivity)
app/src/main/java/com/shavlegi/docreader/
  MainActivity.kt                 (SAF OpenDocument launcher, persistable Uri permission, screen switch)
  model/DocType.kt                (format enum + colours, RecentFile @Serializable)
  data/RecentFilesStore.kt        (DataStore preferences, max 20 recents)
  data/DocumentLoader.kt          (POI: DOCX/XWPF, DOC/HWPF, XLSX+XLS/WorkbookFactory,
                                   PPTX/XSLF, TXT/MD/CSV read + TXT/MD save-back)
  data/PdfDoc.kt                  (PdfRenderer wrapper, Mutex-serialised page rendering)
  ui/DocViewModel.kt              (openFile/load/save, recents flow, error enum)
  ui/Components.kt                (FormatBadge, formatSize, formatDateLabel)
  ui/HomeScreen.kt                (open button, search, filter chips, recents list, clear-all)
  ui/PdfViewer.kt                 (vertical pager, zoom buttons 1x/1.5x/2x, "N / total" chip)
  ui/OfficeViewer.kt              (docx paragraphs+tables, xlsx sheet tabs+grid, pptx slides)
  ui/TextViewer.kt                (read with SelectionContainer; edit with BasicTextField)
  ui/ViewerScreen.kt              (top bar, edit/save/discard dialog, BackHandler, content dispatch)
  ui/theme/Theme.kt               (dark Material 3, same palette as fake-gps: #0B0F19 / #4C8DFF)
app/src/main/res/values/strings.xml       (Georgian, 38 keys)
app/src/main/res/values-en/strings.xml    (English, same 38 keys)
app/src/main/res/values-ru/strings.xml    (Russian, same 38 keys)
app/src/main/res/values/themes.xml        (Theme.DocReader, android:Theme.Material.NoActionBar)
app/src/main/res/drawable/ic_launcher_foreground.xml (white page + blue lines, brand blue)
app/src/main/res/drawable/ic_launcher_background.xml (dark #0B0F19)
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml + ic_launcher_round.xml (adaptive icon)
```

## Exact dependency versions (app/build.gradle.kts)

- AGP 8.5.2, Kotlin 2.0.21, Gradle wrapper 8.7, JDK 17 (all mirrored from fake-gps-app)
- Compose BOM 2024.09.00; core-ktx 1.13.1; activity-compose 1.9.2
- lifecycle-viewmodel-compose 2.8.5; lifecycle-runtime-compose 2.8.5
- datastore-preferences 1.1.1; kotlinx-serialization-json 1.7.3
- **org.apache.poi:poi-ooxml:5.2.5** (verified on Maven Central, 200) — .docx/.xlsx/.pptx
- **org.apache.poi:poi-scratchpad:5.2.5** (verified on Maven Central, 200) — old .doc/.xls (HWPF/HSSF)
- **com.android.tools:desugar_jdk_libs:2.0.4** — verified as a JAR on Google Maven
  (NOT on Maven Central — 404 there; Gradle resolves it from google()).
  `coreLibraryDesugaringEnabled = true`.
- No INTERNET permission; no appcompat dep (ComponentActivity + android:Theme.Material.NoActionBar).

## Verification done on this VM (no Android SDK — no local build)

- All XML files parsed with Python's xml parser: well-formed (manifest, strings ×3, themes, icons).
- Kotlin files: read back carefully + brace/paren balance check; imports cross-checked
  against the APIs used (POI, Compose, DataStore, PdfRenderer).
- Gradle files checked line-by-line against fake-gps-app (same plugin versions, same
  workflow steps, same JVM/JDK setup).
- POI 5.2.5 + scratchpad 5.2.5 + desugar 2.0.4 existence verified with HTTP 200
  against repo1.maven.org / dl.google.com (2026-11-24).

## First build — fix applied 2026-11-25 02:04
- Run #1 (commit dbaf433) **FAILED** at `compileDebugKotlin`: unresolved `FolderOpen` (HomeScreen), `Remove` (PdfViewer), `Save` (ViewerScreen). Cause: no icons dependency — material3 only pulls in `material-icons-core`, and those three icons live in `material-icons-extended`.
- Fix (commit 8feb4724): added `implementation("androidx.compose.material:material-icons-extended")` to `app/build.gradle.kts`. Run #2 triggered by that push — check it before reporting the APK.

## v1.1 — PDF viewer crash fix (2026-11-25 02:25, Shao's „კი, გაასწორე")
- Shao's test: opening the 47-page Facebook Ads course PDF crashed the app („Docs keeps stopping"); the file itself is fine (47 pp, A4, WeasyPrint, 198 KB) and recents worked.
- Root cause (most likely, from code review): `PdfPageItem` recycled the old bitmap in `DisposableEffect(bitmap)` whenever a new render finished — if Compose was still drawing the old bitmap, Android crashes with „trying to use a recycled bitmap". Second factor: render width cap was 2400 px (~26 MB/page at zoom 2x).
- Fix in `PdfViewer.kt`: (1) recycle ONLY when the page leaves the list (`rememberUpdatedState` + `DisposableEffect(Unit)`); a replaced bitmap is left for GC. (2) render width cap 2400 → 1600 px.
- Release v1.1 after the build goes green (tag v1.1, same release flow as v1.0).

## Known risks / things I am unsure of (check before/after first build)

1. **POI on Android — duplicate classes / META-INF.** packagingOptions excludes the
   usual META-INF files. The most likely first-build failure is a duplicate-file
   error from POI's transitive deps (xmlbeans, commons-compress, commons-io,
   log4j-api). If the build fails on a duplicate, add a `pickFirsts` line for
   the named file — do NOT exclude `META-INF/services/**`.
2. **POI size.** Expect the debug APK around 25–35 MB (POI + xmlbeans schemas).
   Release minify is OFF (same as the reference apps) to avoid POI reflection
   breakage; turning R8 on later needs POI keep-rules.
3. **HWPF (.doc) text is rough.** `HWPFDocument.documentText` returns plain text
   with `\r` separators; no tables, no formatting. Acceptable for v1 reading.
4. **XLSX via WorkbookFactory** handles both .xlsx and .xls with one code path;
   if a weird .xls fails, fall back to explicit HSSFWorkbook — noted, not coded.
5. **PdfRenderer memory.** Pages render on demand and recycle on dispose, but a
   very long PDF scrolled fast will churn bitmaps; max render width is capped
   at 2400 px. If OOM appears on the Poco F4, lower the cap to 1600.
6. **Pinch zoom is NOT implemented** — v1 has zoom buttons (1x/1.5x/2x) + horizontal
   scroll when zoomed. Pinch is a v1.1 item.
7. **SAF write permission.** `takePersistableUriPermission` with WRITE is
   best-effort (some providers grant read-only); saving then fails with the
   "save failed" toast. Google Drive / Downloads providers normally grant write.
8. **No ACTION_VIEW intent-filter in v1** (open files from other apps) — deliberate,
   per scope. Easy v1.1 addition: intent-filter + handle `intent.data` in MainActivity.
9. **Strings:** `R.string.today/yesterday` formatting uses device locale for the
   "d MMM" fallback — fine.
10. **First Actions run is the real test.** No local build was possible here
    (no Android SDK on this VM). If `assembleDebug` fails, the error will point
    at one of the risks above — most likely (1).

## What the parent must check before pushing

- [ ] Repo https://github.com/Shavlegi/doc-reader is still empty on `main`
      (if Shao or someone pushed something meanwhile, do NOT overwrite — reconcile).
- [ ] Push all files above (including `.github/workflows/android.yml` and the
      gradle wrapper — the wrapper jar is required for Actions to run).
- [ ] Watch the first Actions run ("Build debug APK") — green means the scaffold
      is sound; fix per risk (1) if it goes red.
- [ ] Download `app-debug.apk` from the run's artifacts, install on the Poco F4,
      and smoke-test: open a real .docx, .xlsx, .pdf, .txt; edit a .txt and save;
      check recents survive an app restart.
- [ ] Tell Shao the APK size — if it is over ~40 MB, mention POI is the reason.
