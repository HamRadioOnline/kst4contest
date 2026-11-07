# KST4Contest

KST4Contest is a Java-based client for the [ON4KST chat](https://www.on4kst.org/chat/login.php), developed for coordinated VHF, UHF and microwave contest operation.

The application is developed by Marc Fröhlich (DO5AMF) and since 2026 Philipp Wagner (DN9APW).

## Start here

- [Project website](https://kst4contest.hamradioonline.de/)
- [Download Stable, Beta and Nightly builds](https://kst4contest.hamradioonline.de/download/)
- [Online manual](https://kst4contest.hamradioonline.de/manual/)
- [GitHub wiki](https://github.com/HamRadioOnline/kst4contest/wiki)
- [Issues and bug reports](https://github.com/HamRadioOnline/kst4contest/issues)
- [Development roadmap](https://kst4contest.hamradioonline.de/roadmap/)

## What KST4Contest does

KST4Contest combines the ON4KST chat with information that is useful when coordinating contacts during a contest.

Among other things, it can:

- display and filter stations from the supported ON4KST chat categories;
- derive band and frequency information from chat messages and station names;
- maintain Worked and NOT QRV information for the available bands;
- calculate station priorities from distance, activity and other available information;
- manage internal skeds and received Win-Test skeds;
- use AirScout information when evaluating possible aircraft-scatter contacts;
- display stations, paths and additional propagation information on maps;
- exchange information with supported logging programs, Win-Test, PSTRotator and a local DX Cluster interface;
- provide configurable automatic replies for recurring chat requests.

Calculated scores, aircraft-scatter information and path assessments are operating aids. They depend on the available data and should not be treated as guarantees that a contact is possible.

## Installation

Ready-to-use packages are available for Windows, Linux and macOS. These packages include the required Java runtime, so a separate Java installation is normally not necessary.

Use the central download page to select the appropriate build:

- **Stable** is intended for normal contest operation.
- **Beta** contains changes that are being prepared for a stable release.
- **Nightly** contains the latest automated development build and is mainly intended for testing.

[Open the download page](https://kst4contest.hamradioonline.de/download/)

## Documentation

The documentation is available in German and English:

- [German manual](https://github.com/HamRadioOnline/kst4contest/wiki/de-Home)
- [English manual](https://github.com/HamRadioOnline/kst4contest/wiki/en-Home)
- [Online manual](https://kst4contest.hamradioonline.de/manual/)

The Markdown sources used for the wiki and the generated PDF manuals are stored in [`github_docs`](github_docs/).

Changes to operating behaviour should be documented together with their purpose and limitations. This is especially important for functions whose result depends on external data, heuristics or information derived from chat messages.

## Building from source

Building KST4Contest requires JDK 21. The Gradle Wrapper included in the repository should be used, so a separate Gradle installation is not required.

Linux and macOS:

```bash
./gradlew clean build
```

Windows:

```powershell
.\gradlew.bat clean build
```

## Repository structure

- `core/` – domain, controller and network code, free of user-interface technology
- `app-desktop/` – the Compose Multiplatform user interface and the packaging tasks
- `github_docs/` – German and English manual sources
- `website/` – project website sources
- `packaging/` – platform-specific packaging files

## CI status

### Documentation

[![Publish wiki](https://github.com/HamRadioOnline/kst4contest/actions/workflows/github-wiki.yml/badge.svg)](https://github.com/HamRadioOnline/kst4contest/actions/workflows/github-wiki.yml)

[![Docs PDF](https://github.com/HamRadioOnline/kst4contest/actions/workflows/docs-pdf.yml/badge.svg)](https://github.com/HamRadioOnline/kst4contest/actions/workflows/docs-pdf.yml)

### Builds

[![Nightly Runtime Artifacts](https://github.com/HamRadioOnline/kst4contest/actions/workflows/nightly-artifacts.yml/badge.svg)](https://github.com/HamRadioOnline/kst4contest/actions/workflows/nightly-artifacts.yml)

## License

KST4Contest is distributed under the [GNU General Public License v3.0](LICENSE).

## Supporters/Sponsor

This Project is possible due to donations of the community. 

You can sponsor KST4Contest here: [Support on Ko-Fi](https://ko-fi.com/praktimarc). If you add an Display Name there you will be mentioned here.

The following people are sponsoring KST4Contest:
- Marco (DJ5KW)
- Steve (G1YBB/M1B/MW1B)
- Jens (DH1AKY)
