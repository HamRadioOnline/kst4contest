# Automatic Chat Suffix Design

## Intent

Every user-visible chat message generated and sent automatically by KST4Contest must end with the fixed suffix ` *a*`. The marker makes automatic traffic recognisable without changing manual messages or ON4KST control traffic.

## Behaviour

- Append ` *a*` to generic private autoanswers, QRG autoanswers, `//VER` replies and both beacon categories.
- Keep the existing `[KST4C Automsg]` prefix on every autoanswer and continue using it for the reply-loop guard.
- Append the suffix unconditionally after text preparation. A configured text which already ends in `*a*` therefore receives another ` *a*`.
- Preserve the existing 120-character beacon payload allowance. The complete transmitted beacon, including the four-character suffix, may contain up to 124 characters.

## Non-goals

- Do not mark manually sent messages, snippets or manually triggered CQ messages.
- Do not alter login, keepalive, ON4KST command or other internal protocol frames.
- Do not change cooldowns, category routing, beacon activation, QRG safeguards, timer behaviour, persistence or settings UI.
- Do not revive or refactor the unused legacy `ScoreboardUpdateTask`.

## Selected Approach

Use one package-private formatter in the controller package to append the fixed suffix. Call it explicitly from the active autoanswer and beacon construction paths. This keeps the scope visible and prevents a broad writer-layer rule from modifying manual messages or raw protocol frames.

## Validation and Documentation

Validate autoanswers after suffix insertion. Validate beacon payloads against 120 characters, append the suffix, then validate the final text against 124 characters. Update focused tests, the German and English manuals, relevant website feature pages, changelogs and durable project context.
