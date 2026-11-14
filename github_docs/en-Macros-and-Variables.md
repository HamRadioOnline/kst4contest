# Macros and Variables

> 🇬🇧 You are reading the English version | 🇩🇪 [Deutsche Version](de-Makros-und-Variablen)

KST4Contest distinguishes between shortcut buttons, text snippets and variables. Shortcuts and snippets contain prepared text. Variables add information which may change during operation.

Inserted text remains visible in the send field and can be checked or edited before transmission.

---

## Overview

| Mechanism | Access | Use |
|---|---|---|
| **Shortcut** | Button above the send field | Inserts configured text into the send field |
| **Snippet** | Context menu or `Ctrl+1` through `Ctrl+0` | Prepares text for the selected station |
| **Variable** | Placeholder within message text | Inserts current QRG, locator, direction, station or AirScout information |

Shortcuts and snippets store text. Variables supply the corresponding current values.

A shortcut such as

```text
pse sked?
```

always inserts the same text. A shortcut containing

```text
pse call me at MYQRGSHORT
```

instead uses the QRG stored in KST4Contest when the button is pressed.

---

## Shortcut Buttons

Shortcuts are configured under **Preferences → Shortcut Settings**.

![Configuration of shortcut buttons and text snippets](client_settings_window_shortcuts.png)

Each entry creates one button in the main window. Pressing it appends the configured text to the existing contents of the send field. Text which has already been prepared is not removed.

If a shortcut contains a variable, the variable is resolved when the text is inserted. For example,

```text
pse call me at MYQRGSHORT
```

may become:

```text
pse call me at 144.388
```

The exact entries `MYQRG` and `SECONDQRG` are highlighted as QRG buttons. They insert the current QRG of the first or second chat category respectively.

The shortcut

```text
/SETNAME MYQRG
```

is highlighted as well. Pressing it resolves `MYQRG` and inserts the resulting server command into the send field. The command is not transmitted automatically.

The order of the entries in the settings determines the button order in the main window. Editing, sorting and saving are described under [Configuration – Shortcut Settings](en-Configuration#shortcut-settings).

---

## Text Snippets

Snippets are configured under **Preferences → Snippet Settings**. They are intended primarily for recurring messages to a particular station.

Snippets can be opened:

- by right-clicking a station in the user list,
- by right-clicking a public message,
- by right-clicking a private message, or
- with `Ctrl+1` through `Ctrl+0` for the first ten entries in the snippet list.

### Using the Context Menu

Selecting a station or message will normally have prepared the corresponding `/cq` destination in the send field. A snippet subsequently chosen from the context menu is appended to this text.

Existing message text can therefore be extended deliberately.

### Using the Keyboard

A snippet invoked with `Ctrl+1` through `Ctrl+0` replaces the previous contents of the send field with a complete directed message:

```text
/cq CALLSIGN snippet text
```

The complete visible callsign, including any suffix, is retained. Selecting `9A0BB-70` may therefore produce:

```text
/cq 9A0BB-70 pse ur qrg?
```

KST4Contest also retains the selected station's chat category internally. A snippet for `9A0BB-70` is therefore not accidentally transmitted through the other active chat category.

If no station is selected, or no snippet exists for the selected key combination, nothing is inserted.

The prepared text is not sent automatically:

- `Enter` or **TX** sends the message.
- `Esc` clears the send field.

### Keyboard Mapping

The mapping follows the order of the snippet list:

| Key combination | Entry used |
|---|---:|
| `Ctrl+1` | first entry |
| `Ctrl+2` | second entry |
| … | … |
| `Ctrl+9` | ninth entry |
| `Ctrl+0` | tenth entry |

The key combinations can also be assigned to a programmable macro keyboard. The idea for this method came from IU3OAR, Gianluca Costantino.

KST4Contest does not define a mandatory set of default snippets. The useful texts depend on the station's own contest operation and operating method.

Editing, sorting and saving are described under [Configuration – Snippet Settings](en-Configuration#snippet-settings).

---

## Variables

Variables are reserved placeholders within message text. They must be written in uppercase and are case-sensitive.

Variables can be used in:

- shortcuts,
- snippets,
- beacon texts, and
- message text entered or pasted directly into the send field.

Variables in a shortcut or snippet are resolved when the text is inserted into the send field. Variables entered or pasted directly into the send field are resolved immediately before the message is placed in the transmission queue.

Station-specific variables always use the currently selected station. KST4Contest does not derive this station from a `/cq` destination entered manually in the message text.

---

## Global Variables

Global variables do not require a selected remote station.

| Variable | Replacement value |
|---|---|
| `MYQRG` | current QRG of the first or primary chat category |
| `MYQRGSHORT` | first seven characters of `MYQRG` |
| `SECONDQRG` | current QRG of the second chat category |
| `MYLOCATOR` | complete locator configured for the local station |
| `MYLOCATORSHORT` | first four characters of the local locator |
| `MYCALL` | configured local callsign |
| `MYQTF` | current antenna direction as a numeric value in degrees |

For example,

```text
cq at MYQRGSHORT, qtf MYQTF, loc MYLOCATOR
```

may be resolved to:

```text
cq at 144.388, qtf 135, loc JO51IJ
```

### QRG Variables

`MYQRG` contains the QRG of the first chat category. The value may come from TRX synchronisation with the logging software or from the manually edited QRG field.

`MYQRGSHORT` uses the same value, but limits it to the first seven characters:

```text
144.388.03 → 144.388
```

`SECONDQRG` contains the QRG of the second chat category. Selecting a station from the second chat does not change the meaning of `MYQRG`. Use `SECONDQRG` explicitly when the QRG of the second category is required.

### Locator Variables

`MYLOCATOR` inserts the complete configured locator of the local station:

```text
JO51IJ
```

`MYLOCATORSHORT` uses only the first four characters:

```text
JO51
```

### MYQTF

`MYQTF` inserts the current antenna direction stored in KST4Contest as a numeric angle in degrees.

For example,

```text
ant MYQTF deg
```

may become:

```text
ant 135 deg
```

The direction is not converted into compass terms such as `north`, `north-east` or `south-west`.

---

## Variables for the Selected Station

These variables require a selected remote station:

| Variable | Replacement value |
|---|---|
| `QRZNAME` | name of the selected station, or its complete callsign if no name is available |
| `FIRSTAP` | description and arrival time of the first aircraft reported by AirScout |
| `SECONDAP` | description and arrival time of the second aircraft reported by AirScout |

For example,

```text
Hi QRZNAME, FIRSTAP, pse lsn at MYQRGSHORT
```

may become:

```text
Hi David, a very big AP in 2 min, pse lsn at 144.388
```

### QRZNAME

KST4Contest uses the name from the selected station's name field. If that field does not contain a usable name, the complete visible callsign is inserted instead.

### FIRSTAP

If an AirScout candidate is available, `FIRSTAP` contains its description and the expected time until the reflection window.

For example:

```text
a very big AP in 2 min
```

If no aircraft is available for the selected station, KST4Contest inserts:

```text
no ap available
```

### SECONDAP

`SECONDAP` uses the second available AirScout candidate.

For example:

```text
Next big AP in 9 min
```

If there is no second candidate, `SECONDAP` is replaced with an empty string.

Further information about the aircraft data is available under [AirScout Integration](en-AirScout-Integration#ap-variables-in-messages).

### Behaviour Without a Selected Station

If no station is selected, `QRZNAME`, `FIRSTAP` and `SECONDAP` remain visible in the text. KST4Contest does not remove these placeholders automatically.

A visible unresolved placeholder is clearer than a formally complete message which silently lacks important information. Before transmission, check that the intended station is selected and that all required variables have been resolved.

---

## Variables in the Beacon

A public beacon has no selected remote station. It can therefore use only global variables:

- `MYQRG`
- `MYQRGSHORT`
- `SECONDQRG`
- `MYLOCATOR`
- `MYLOCATORSHORT`
- `MYCALL`
- `MYQTF`

`QRZNAME`, `FIRSTAP` and `SECONDAP` are not resolved in a beacon and should not be used there.

A possible template for the first chat category is:

```text
calling cq at MYQRGSHORT, ant MYQTF deg, loc MYLOCATOR
```

If the second chat category uses a different QRG, its template must contain `SECONDQRG`:

```text
calling cq at SECONDQRG, ant MYQTF deg, loc MYLOCATOR
```

Global variables are evaluated again on every timer run. `MYQRG` and `MYQRGSHORT` always require a valid primary-category QRG, while `SECONDQRG` always requires a valid secondary-category QRG, regardless of which beacon template contains them. If a required QRG is unavailable, only the affected beacon cannot be enabled or is disabled. A template without a QRG variable may still be enabled without an available QRG.

When enabled, each beacon remembers the QRG of its own category. A manual QRG change moves this reference. If UCXLog or Win-Test later changes the assigned QRG by more than 5 kHz, KST4Contest disables the affected beacon instead of silently including the new frequency in its next transmission. If no own QRG exists when the beacon is enabled, the first valid frequency entered or received later becomes the reference.

The fully resolved beacon content before the fixed ` *a*` suffix:

- must contain at least one valid character,
- must not exceed 120 characters,
- must not contain the protocol separator `|`, and
- must not contain line breaks.

KST4Contest appends the suffix automatically afterwards. The transmitted beacon message may therefore contain up to 124 characters.

If the resolved text is empty or invalid for another reason when transmission is due, that beacon run is skipped and the status chip shows **FAILED**.

The interval, activation and behaviour of both categories are described under [Configuration – Beacon Settings](en-Configuration#beacon-settings).

---

## Example Snippet Workflow

For example, the first configured snippet may contain:

```text
Hi QRZNAME, pse sked? I call at MYQRGSHORT
```

The workflow can then look like this:

1. Select `DL1ABC-432` in the user list.
2. Press `Ctrl+1`.
3. KST4Contest prepares the directed message and resolves its variables.
4. Check the complete text in the send field.
5. If the remote station has proposed another QRG, edit the text accordingly.
6. Press `Enter` or **TX** to send the message.

The result may be:

```text
/cq DL1ABC-432 Hi Peter, pse sked? I call at 432.088
```

The complete callsign determines the recipient. The selected chat category determines the transmission path. Variables reduce repeated typing, but they do not decide whether the inserted information still matches the current operating situation.

---

## Limits of Variable Resolution

Variables reflect the information available to KST4Contest at the time they are resolved.

In particular:

- A QRG supplied by the logging software may have changed in the meantime.
- A manually entered QRG remains active until it is changed again.
- `MYQRG` remains the QRG of the primary category even if a station from the second category is selected.
- The selected station may differ from a `/cq` destination entered manually.
- AirScout may not provide current aircraft data for the path in question.
- Station-specific variables remain visible when no station is selected.
- Inserted text is not checked automatically for operational correctness.

The send field therefore remains editable after a shortcut or snippet has been inserted. Variables avoid repeated input; the final check remains the operator's responsibility.

---

## Use During a Contest

Shortcuts, snippets and variables are individual tools within the operating workflow. Their interaction with CQ operation, station selection, skeds, QRG changes and log synchronisation is described under [Contest Workflow with KST4Contest](en-Contest-Workflow).
