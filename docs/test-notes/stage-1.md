# Stage 1 test notes

Commit: `<fill in at tag time>`   Tag: `stage-1`   JDK: 21 (compiled with `--release 17`)   OS: Windows 11

## How to reproduce

```
build.bat
run.bat scripts\stage1-victory.txt
run.bat scripts\stage1-defeat-early-stop.txt
run.bat scripts\stage1-defeat-idle.txt
test.bat
```

(macOS/Linux: `sh build.sh`, `sh run.sh scripts/stage1-victory.txt`, `sh test.sh`.)

Each script holds two commands per turn (Waiter, then Chef); `#` lines are comments. The game echoes every
scripted command after its prompt, so the output reads like a live session. If a script is shorter than the
game, the remaining prompts are read from the keyboard.

`test.bat` runs the automated suite: unit tests per rule, the whole-flow replays, the three scripts above through
the real controller and views, a command-parser table, and 400 seeded random games that check structural
invariants (scoreboard totals, table/occupant consistency, satisfaction range, one payment per turn, locked prices).

## Run 1 - Victory (`scripts/stage1-victory.txt`)

| Turn | Arrival | Waiter | Chef | Automatic events |
|------|---------|--------|------|------------------|
| 1 | C1 | seat C1 -> T1 | wait | |
| 2 | | order C1 (Salad) | cook C1 -> READY | |
| 3 | | serve C1 | wait | |
| 4 | C2 | seat C2 -> T2 | wait | C1 ready to pay |
| 5 | | order C2 (Burger) | cook C2 -> READY | C1 pays $9.00 (sat 84), T1 free |
| 6 | | serve C2 | wait | |
| 7 | C3 | seat C3 -> T1 | wait | C2 ready to pay |
| 8 | | order C3 (Pasta) | cook C3 (1/2) | C2 pays $15.00 (sat 84), T2 free |
| 9 | | wait | cook C3 -> READY | |
| 10 | C4 | serve C3 (C4 waits) | wait | |
| 11 | | seat C4 -> T2 | wait | C3 ready to pay |
| 12 | | order C4 (Salad) | cook C4 -> READY | C3 pays $18.00 (sat 76), T1 free |
| 13 | C5 | serve C4 (C5 waits) | wait | |
| 14 | | seat C5 -> T1 | wait | C4 ready to pay |
| 15 | | order C5 (Burger) | cook C5 -> READY | C4 pays $9.00 (sat 76), T2 free |
| 16 | C6 | serve C5 (C6 waits) | wait | |
| 17 | | seat C6 -> T2 | wait | C5 ready to pay |
| 18 | | order C6 (Pasta) | cook C6 (1/2) | C5 pays $15.00 (sat 76), T1 free |

Final: 5 paid, $66.00, average satisfaction 79.2, 0 departures -> **VICTORY**
(checked automatically: `ScriptedRunTest.victory`, `FlowTest.fullVictoryLine`).

## Run 2 - Defeat, early stop (`scripts/stage1-defeat-early-stop.txt`)

Turns 1-10 are identical to Run 1, then both staff `wait` for turns 11-18.

Final: C1, C2, C3 paid -> 3 paid, $42.00, average 81.3; C4/C5/C6 still waiting at satisfaction 28 / 52 / 76;
0 departures -> **DEFEAT** (fails "paid >= 4" and "revenue >= $45.00" although satisfaction passes).

## Run 3 - Defeat, idle (`scripts/stage1-defeat-idle.txt`)

Both staff wait on all 18 turns.

Final: 0 paid, $0.00, average N/A, 2 unhappy departures (C1 leaves at the end of turn 13, C2 at the end of
turn 16; C3 is still waiting at satisfaction 4) -> **DEFEAT**. This exercises the abandonment rule.

## Invalid-input checks (task must not be consumed)

Messages are exactly what the game prints. Each row was either typed into a live session or asserted by an
automated test (`StaffTest`, `RestaurantTest`, `ScriptedRunTest`). In every row the same staff member is prompted
again on the same turn.

| Prompt | Input | Message printed | Task consumed? |
|--------|-------|-----------------|----------------|
| Waiter | `order 1` (C1 still queued) | C1 is still in the queue. Seat them first. | No |
| Waiter | `seat 9` | There is no customer C9. | No |
| Waiter | `seat 1 5` | There is no table T5. | No |
| Waiter | `bogus` | Unknown command 'bogus'. Waiter can: seat, order, serve, wait. Type 'help'. | No |
| Waiter | `cook 1` | 'cook' is a Chef task. Waiter can: seat, order, serve, wait. | No |
| Waiter | `seat` | 'seat' needs a customer id, e.g. 'seat 3' or 'seat 3 1'. | No |
| Waiter | `seat 3` with both tables full | No free table. A table frees up when its customer pays or leaves. | No |
| Waiter | `seat 3 1` with T1 occupied by C1, T2 free | Table 1 is occupied by C1. Free tables: 2. | No |
| Waiter | `seat 2` when C2 is already seated | C2 is not waiting in the queue (status: SEATED). | No |
| Waiter | `order 1` twice | C1 has already ordered Salad (PLACED 0/1). | No |
| Waiter | `serve 3` while Pasta is cooking | C3's Pasta is not READY (1/2 units done). | No |
| Chef | `order 1` | 'order' is a Waiter task. Chef can: cook, wait. | No |
| Chef | `cook 1` before any order | C1 has no order to cook yet. | No |
| Chef | `cook 1` on a READY order | C1's Salad is READY and does not need cooking. | No |
| Chef | `cook 1` after C1 left | C1's Salad is CANCELLED and cannot be cooked. | No |
| Either | empty line | Please type a command. ... Type 'help'. | No |
| Either | stdin closed | (input closed - remaining turns will wait); game finishes | n/a |

## Edge cases verified

- A leading byte-order mark (added by Windows Notepad and some PowerShell pipes) is ignored in typed and scripted input.
- Windows (CRLF) line endings in scripts and piped input.
- End of input at any point finishes the game as a defeat, never a crash.
- Customer walks out while their order is cooking: order cancelled, table freed, no revenue.
- Two customers ready to pay on the same turn: only the lower id pays, the other pays the next turn.
- Customer served on turn t is ready to pay on t+1 and pays on t+2 at the earliest.
- Satisfaction floors at 0; customers who are eating, ready to pay, paid or gone lose nothing.
- Victory boundaries: exactly 4 paid / $45.00 / average 60 wins; 3 paid, $44.99 or average 59.75 lose.
- Missing script file or too many arguments: message on stderr, exit code 2.

## Known intended behaviour

- The turn-16 customer cannot pay within 18 turns, even with perfect play (brief section 3.4).
- A typo inside a script file uses up one script line, so later lines shift by one. Scripts must contain valid commands only.
- When `quit` is confirmed inside a script, the next script line is read as the y/n answer.
