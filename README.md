# Restaurant Rush (SC2002 group assignment)

A turn-based restaurant management game for the command line, written in Java. You are the manager: each turn
you give one command to the Waiter and one to the Chef, and the Cashier works on its own. Serve enough happy
customers within 18 turns to win.

## Project Overview
[![Architecture diagram of a-sideffect/sc2002_restaurant_rush](https://gitdiagram.com/a-sideffect/sc2002_restaurant_rush/diagram.png)](https://gitdiagram.com/a-sideffect/sc2002_restaurant_rush?utm_source=readme&utm_medium=picture)

This README describes **Stage 2**: the Stage 1 base game plus VIP and Critic customers. No Host, Combo Meal or
Happy Hour features are included at this stage.

## Requirements

- JDK 17 or newer (developed on JDK 21, compiled with `--release 17`)
- No other dependencies. The game runs fully offline.

## Build, run, test

| Task | Windows | macOS / Linux |
|------|---------|---------------|
| Build | `build.bat` | `sh build.sh` |
| Play | `run.bat` | `sh run.sh` |
| Replay a script | `run.bat scripts\stage2-victory.txt` | `sh run.sh scripts/stage2-victory.txt` |
| Run all tests | `test.bat` | `sh test.sh` |

Build first; `run` and `test` use the compiled `out/` and `out-test/` folders.

## The base setting

- 18 turns, 2 tables, one Waiter, one Chef, one Cashier.
- Customers arrive on turns 1, 4, 7, 10, 13 and 16 with satisfaction 100.
- Customer types: C2 (turn 4) is a **VIP** and C4 (turn 10) is a **Critic**; the rest are Regular.

  | Type | Satisfaction lost per turn while waiting | Bill | Dish |
  |------|------------------------------------------|------|------|
  | Regular | 8 | full price | rule F1 |
  | VIP | 5 | 10% off | rule F1 |
  | Critic | 12 | full price | always the highest-priced dish |

- Menu: Salad $9.00 (1 unit), Burger $15.00 (1 unit), Pasta $18.00 (2 units).
- **Win** at the end of turn 18: at least 4 customers paid, revenue of at least $45.00, and an average satisfaction
  of paid customers of at least 60. The game always plays all 18 turns.

### Turn order

1. Arrivals join the waiting queue.
2. You command the Waiter (one task), then the Chef (one preparation unit).
3. The Cashier collects from at most one customer who has been ready to pay since an earlier turn (oldest first).
4. Customers served on an earlier turn finish eating and become ready to pay.
5. Customers still waiting, seated or ordered lose satisfaction by their type (never below 0). At 0 they leave and their order is cancelled.
6. After the last turn, the result is shown.

## Controls

| Prompt | Command | Meaning |
|--------|---------|---------|
| Waiter | `seat <c> [t]` | Seat customer c at table t (default: lowest free table) |
| Waiter | `order <c>` | Take c's order; the customer chooses the dish |
| Waiter | `serve <c>` | Serve c's READY order |
| Waiter / Chef | `wait` (or `w`) | Do nothing this turn |
| Chef | `cook <c>` | Advance c's order by one unit |
| Either | `help`, `status`, `menu` | Show help, reprint the board, show the menu (no turn used) |
| Either | `quit` | Leave the game after confirming |

Ids can be written `3` or `C3`, tables `2` or `T2`; commands are not case-sensitive.
An invalid command prints why and asks again. It never uses up the staff member's turn.

## How customers choose food (Rule F1)

Customer Cn orders menu item number `(n - 1) mod menuSize`. On the base menu that gives
C1 Salad, C2 Burger, C3 Pasta, C4 Salad, C5 Burger, C6 Pasta. The rule is deterministic so runs are reproducible.
You see the dish when you take the order. It lives in `Customer.chooseDish(Menu)`, so a customer type can override it:
the Critic does, so C4 orders Pasta instead of Salad.

## Reproducible runs

| Script | Result |
|--------|--------|
| `scripts/stage2-victory.txt` | VICTORY: 5 paid, $73.50, average 75.6 |
| `scripts/stage2-defeat-early-stop.txt` | DEFEAT: 3 paid, $40.50, average 83.3, 1 departure |
| `scripts/stage2-defeat-idle.txt` | DEFEAT: 0 paid, 2 unhappy departures |

The `stage1-*` scripts reproduce the Stage 1 results at git tag `stage-1` (all customers Regular).
Details and turn-by-turn tables are in `docs/test-notes/stage-2.md` and `docs/test-notes/stage-1.md`.
The turn-16 customer can never pay within 18 turns. That is intended.

## Project layout

```
src/main/java/rush/
  Main.java            wiring and command-line arguments
  domain/              game rules and state (no input/output)
  control/             GameController (turn loop), ManagerInput, TurnReport
  ui/                  ConsoleView, ConsoleInput, ScriptedInput, CommandParser
src/test/java/rush/    dependency-free tests (TestRunner)
scripts/               replayable command scripts
docs/                  test notes and design log
Stage1_Development_Plan.md   the plan this stage was built from
```

Dependencies only point one way: `ui` and `control` use `domain`; `domain` never imports them.
Money is stored as integer cents everywhere and formatted only for display.
