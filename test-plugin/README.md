# Mlib Chat Test Plugin

A small manual-test harness that exercises Mlib's [`Chat`](../docs/Chat.md) system on
a real server, using [`run-paper`](https://github.com/jpenilla/run-task) to download
and launch Paper.

> **Not part of the published library.** This is a standalone **Gradle** project that
> the root **Maven** build never references, so JitPack (which builds the Maven root)
> ignores it entirely. Nothing here ends up in the `dev.mrshawn:mlib` artifact.

## Prerequisites

- A JDK (17+; the build targets Java 17 bytecode and runs fine on newer JDKs).
- The Mlib artifact installed to your local Maven repo (the plugin resolves it via
  `mavenLocal()`).

## Running

```bash
# 1. From the repository root, build + install Mlib into ~/.m2
mvn install

# 2. Launch a Paper server with the test plugin
cd test-plugin
./gradlew runServer
```

On first launch the server stops at the Mojang EULA. Accept it once by setting
`eula=true` in `test-plugin/run/eula.txt`, then run `./gradlew runServer` again.

> If you bump the library version, update `mlibVersion` in `build.gradle.kts` to
> match the root `pom.xml` and re-run `mvn install`.

## What it checks

On enable it logs the selected platform — on Paper you should see:

```
[MlibChatTest] Paper-native Adventure available: true
[MlibChatTest] Active chat platform: PaperNativePlatform
[MlibChatTest] Platform send OK via PaperNativePlatform
```

(On Spigot it falls back to `BukkitAudiencesPlatform`.)

Then use `/chattest <sub>` in-game or from the console:

| Subcommand | Exercises |
|------------|-----------|
| `legacy` | `&` codes + `&#RRGGBB` hex |
| `mini` | MiniMessage (`<rainbow>`, `<gradient>`, …) |
| `actionbar` | action bar |
| `title` | title + subtitle with timings |
| `broadcast` | server-wide broadcast |
| `clear` | clear the sender's chat |
| `template` | `TextMessage` + `TextReplacement` |
| `papi` | PlaceholderAPI (install the plugin to see it resolve) |
| `platform` | report the active platform |

Type `/chattest` with no argument for the in-game help list.
