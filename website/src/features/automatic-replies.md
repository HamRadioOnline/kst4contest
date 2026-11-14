---
title: Automatic Private Replies
icon: ↩️
category: ON4KST Chat
since: "1.25"
summary: Answer repeated private messages or QRG requests without losing the complete callsign, chat category or protection against automatic reply loops.
description: KST4Contest can send a predefined answer to incoming private messages and provide the QRG belonging to the category in which a request was received.
tagsList:
  - ON4KST
  - automatic reply
  - private message
  - QRG
  - sked request
  - dual chat
related:
  - private-message-handling
  - trx-qrg-synchronisation
  - dual-chat
  - macros
  - sked-reminder
---

## Why use an automatic reply?

Not every station visible in the ON4KST chat is taking part in the current contest. Some operators may be monitoring activity, testing their station or simply remaining logged in while not accepting skeds.

That distinction is not always checked before requests are sent. During a busy contest, the same station may therefore receive several similar private messages and have to enter the same refusal repeatedly.

KST4Contest can provide that answer automatically while leaving the incoming message visible.

## Two separate reply functions

The general automatic reply sends one configured text in response to an incoming private message. A practical example is:

```text
Sri, I am not taking part in this contest. No skeds.
```

The QRG reply serves a different purpose. It recognises common questions such as:

```text
qrg?
freq?
pse qrg
```

and answers with the QRG belonging to the chat category in which the request was received:

```text
[KST4C Automsg] QRG is: 144.300.00 *a*
```

When both functions are enabled, a recognised QRG request receives the QRG reply only. It does not additionally trigger the general answer.

## Category and callsign remain part of the message

An automatic reply is addressed to the complete callsign of the sender, including any visible suffix. It is also sent through the same chat category as the incoming message.

A request from `CALLSIGN-70` is therefore not silently redirected to `CALLSIGN`, and a message received through the microwave chat is not answered through the primary VHF/UHF category.

This is particularly important in station setups which use separate suffixes for different bands or operating positions.

## Missing information is not useful information

KST4Contest sends a QRG reply only when a QRG is available for the relevant category. It does not produce an answer containing only:

```text
QRG is:
```

A general answer must likewise contain actual text and must not contain characters which would break the ON4KST protocol frame.

Rejecting these replies locally is intentional. An automatic response which contains no usable information has saved nobody any work.

## Preventing loops and repeated answers

Every automatic reply contains:

```text
[KST4C Automsg]
```

Messages which already contain this prefix are not answered automatically. Two KST4Contest clients therefore do not continue replying to each other.

Every chat message generated automatically by KST4Contest also ends with ` *a*`. This includes general and QRG-specific replies, `//VER` replies and both beacons. The suffix is added even when the configured text already ends with it; manual messages are unchanged.

A shared two-minute cooldown additionally applies to the general and QRG-specific functions. It is tracked separately for each complete callsign and chat category.

A rejected reply does not start the cooldown. If a QRG was missing and is entered afterwards, the next request can be answered immediately.

## Operational limits

The function reacts to incoming text. It does not decide whether the sender's request was reasonable, whether a sked could be possible later or whether the configured refusal still reflects the current operating status.

The general reply should therefore be enabled only while its text remains correct. A message which says that the station is not participating becomes misleading if the operator starts working the contest half an hour later.

In plain terms: automatic replies remove repetitive typing. They do not take over the conversation.

[Read the complete configuration and recognised QRG requests in the manual.](/manual/en/configuration/#messagehandling-settings-from-v125)

[Read how two chat categories and suffixed callsigns are kept separate.](/features/dual-chat/)
