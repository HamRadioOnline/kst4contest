# KST4Contest – User Manual

> You are reading the English version | [Deutsche Version](de-Home)

KST4Contest is a desktop client for the [ON4KST Chat](https://www.on4kst.org/chat/login.php), developed for VHF, UHF and SHF contest operation. It brings chat, candidate selection, sked planning, aircraft scatter data and external station software together in a single operating interface.

KST4Contest is developed by **DO5AMF (Marc Fröhlich)**, operator at DM5M and (since May 2025) **DN9APW (Philipp Wagner)**. The source code is publicly available on [GitHub](https://github.com/HamRadioOnline/kst4contest).

---

## Why use a dedicated ON4KST client?

During a contest, the ON4KST Chat provides a considerable amount of information: active stations, locators, frequencies, sked requests and indications of current activity. The actual problem is not seeing this data. It is turning it into the next useful contact in time.

KST4Contest evaluates the available information, puts it into context and presents it as part of a contest-oriented workflow. This includes antenna direction, distance, known bands and frequencies, worked status, chat activity and aircraft scatter timing.

The program does not decide which QSO is actually possible. Priority scores, the AP timeline and visual highlights are decision aids. The final judgement remains with the operator – not least because even a very convincing computer screen cannot complete a radio contact.

---

## How KST4Contest supports contest operation

- **Observe and organise chat activity:** KST4Contest can display two ON4KST chat categories at the same time. Messages, frequency information and known band activity are assigned to the corresponding stations.

- **Reduce the station list:** Direction, distance, worked and NOT-QRV filters help limit the user list to stations that are relevant to the current operating situation.

- **Prioritise candidates:** The score system evaluates active chat members using several known criteria. The currently most relevant candidates are also shown in a separate priority list.

- **Prepare skeds and keep them visible:** Sked reminders, automatic advance messages and the AP timeline help prevent scheduled contacts from disappearing somewhere between chat, logging and ongoing CQ operation.

- **Include aircraft scatter information:** The AirScout interface brings suitable aircraft and expected reflection times into candidate evaluation and sked planning.

- **Connect the logger and station equipment:** KST4Contest synchronises worked stations and frequency information with supported logging software. It also provides interfaces for Win-Test, PSTRotator and a built-in DX Cluster server.

- **Display stations and radio paths:** The station map shows active chat members, locator squares, antenna directions and the path to the selected station. A terrain profile can also be calculated for selected paths.

These functions share information. A detected frequency can indicate an active band, worked status comes from the logger, aircraft scatter data adds timing information and the resulting score affects the priority list. Missing or outdated input data can therefore affect the result as well.

---

## Requirements

A registered ON4KST account is required. Registration and login are available from the [official ON4KST login page](https://www.on4kst.org/chat/login.php).

English is the official language of the ON4KST Chat. This also applies when communicating with stations from your own country. Common amateur radio abbreviations such as `pse`, `agn`, `qrg`, `dir`, `rrr`, `tnx` and `73` are normal and usually considerably faster than carefully written prose.

Downloads, supported operating systems and installation methods are described in [Installation](en-Installation).

---

## Manual version

This manual describes the current stable release of KST4Contest.

The currently published Stable version is **v1.42.0**.

Functions that are only available in a Beta or Nightly build are marked accordingly. If no such note is present, the description applies to the stable release.

- [Download Stable, Beta and Nightly builds](https://kst4contest.hamradioonline.de/download/)
- [GitHub releases](https://github.com/HamRadioOnline/kst4contest/releases)
- [Version history](en-Changelog)

The stable release is normally the appropriate choice for contest operation. Beta and Nightly builds contain newer fixes and functions, but may still change between builds. They are intended for testing specific changes. Ten minutes before a contest is usually not the ideal time for a first test.

---

## Quick navigation

| Page | Contents |
|---|---|
| [Installation](en-Installation) | ON4KST account, downloads, installation and updates |
| [Configuration](en-Configuration) | Login, station, bands, user interface and external connections |
| [Contest Workflow](en-Contest-Workflow) | Pre-start checks, CQ operation, candidate selection, skeds, logging and band changes |
| [Log Synchronisation](en-Log-Sync) | Simplelogfile, UCXLog, N1MM+, QARTest, DXLog.net and Win-Test |
| [AirScout Integration](en-AirScout-Integration) | Connecting AirScout and evaluating aircraft scatter timing |
| [DX Cluster Server](en-DX-Cluster-Server) | Passing detected opportunities to logging software |
| [Features](en-Features) | Operation, reasoning and limitations of individual functions |
| [Macros and Variables](en-Macros-and-Variables) | Reusable messages, shortcuts and automatically substituted values |
| [User Interface](en-User-Interface) | Interface layout and operation during a contest |
| [Changelog](en-Changelog) | Releases, Nightly changes and resolved issues |

---

## Contact and support

- **Download:** [Stable, Beta and Nightly builds](https://kst4contest.hamradioonline.de/download/)
- **Source code:** [HamRadioOnline/kst4contest](https://github.com/HamRadioOnline/kst4contest)
- **Bug reports and feature requests:** [GitHub Issues](https://github.com/HamRadioOnline/kst4contest/issues)
- **Email:** praktimarc+kst4contest@gmail.com  
  Please use this address only for KST4Contest-related topics.

### Reporting a bug

A problem is considerably easier to reproduce when the report contains at least:

1. the KST4Contest version,
2. the operating system and installation method,
3. the exact steps leading to the problem,
4. the expected and observed behaviour,
5. a screenshot where appropriate,
6. the error log file.

The error log is stored at:

| Operating system | Path |
|---|---|
| Linux / macOS | `~/.praktiKST/debug/kst4contest-errors.log` |
| Windows | `C:\Users\<YourName>\.praktiKST\debug\kst4contest-errors.log` |

Please inspect the file briefly before uploading it. An error log mainly contains technical information, but depending on the problem it may also include local file paths or other contextual data.

When **Settings → Messagehandling → Enable debug mode to file** is enabled, the `debug` directory also contains `Messagehistory.raw` and possibly archived `Messagehistory-*.raw` files. These files record the complete ON4KST session traffic in both directions. Login passwords are redacted, but chat and station data remains present. Share such a file only after reviewing it deliberately.

Some file and directory names still use the technical name `praktiKST`. They refer to the same program.

---

## Acknowledgements

Many functions and corrections in KST4Contest originate from observations made during actual contest operation. Reports that describe not only what happened, but also under which conditions it happened, are particularly useful.

Special thanks go to:

- Gianluca Costantino (IU3OAR)
- Alessandro Murador (IZ3VTH)
- Reczetár István (HA1FV)
- Viliam Petrik (OM0AAO) for the DX Cluster idea
- Konrad Neitzel (DC9DJ) for his work on the project structure
- Andreas (DO5ALF), webmaster of funkerportal.de
- Franz van Velzen (PE0WGA) for testing
- Philipp (DN9APW) for further development of KST4Contest and the CI/CD infrastructure
- all other testers and contributors who supplied reproducible reports, ideas and corrections

Not every suggestion can be implemented unchanged. Nevertheless, reports from real operation remain an important basis for deciding which problems should be solved first.
