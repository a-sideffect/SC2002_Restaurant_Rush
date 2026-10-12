# Restaurant Rush (SC2002 group assignment)

A turn-based restaurant management game for the command line, written in Java. You are the manager: each turn
you give one command to the Waiter, the Host and the Chef, and the Cashier works on its own. Serve enough happy
customers within 18 turns to win.

## Project Overview
[![Architecture diagram of a-sideffect/sc2002_restaurant_rush](https://gitdiagram.com/a-sideffect/sc2002_restaurant_rush/diagram.png)](https://gitdiagram.com/a-sideffect/sc2002_restaurant_rush?utm_source=readme&utm_medium=picture)

The game was built in three stages, each with its own git tag and test notes:

| Stage | Adds | Tag | Notes |
|-------|------|-----|-------|
| 1 | Base game: Regular customers, Waiter, Chef, Cashier | `stage-1` | `docs/test-notes/stage-1.md` |
| 2 | VIP and Critic customers | `stage-2` | `docs/test-notes/stage-2.md` |
| 3 | Host, Combo Meals, Happy Hour | `stage-3` | `docs/test-notes/stage-3.md` |

This README describes the current (Stage 3) game.

## Requirements

- JDK 17 or newer (developed on JDK 21, compiled with `--release 17`)
- No other dependencies. The game runs fully offline.

## Build, run, test

| Task | Windows | macOS / Linux |
|------|---------|---------------|
| Build | `build.bat` | `sh build.sh` |
| Play | `run.bat` | `sh run.sh` |
| Replay a script | `run.bat scripts\stage3-victory.txt` | `sh run.sh scripts/stage3-victory.txt` |
| Run all tests | `test.bat` | `sh test.sh` |

Build first; `run` and `test` use the compiled `out/` and `out-test/` folders.

## The base setting

- 18 turns, 2 tables, one Waiter, one Host, one Chef, one Cashier.
- Customers arrive on turns 1, 4, 7, 10, 13 and 16 with satisfaction 100.
- Customer types: C2 (turn 4) is a **VIP** and C4 (turn 10) is a **Critic**; the rest are Regular.

  | Type | Satisfaction lost per turn while waiting | Bill | Dish |
  |------|------------------------------------------|------|------|
  | Regular | 8 | full price | rule F1 |
  | VIP | 5 | 10% off | rule F1 |
  | Critic | 12 | full price | always the highest-priced item |

- Menu:

  | Item | Price | Chef units |
  |------|-------|-----------|
  | Salad | $9.00 | 1 |
  | Burger | $15.00 | 1 |
  | Pasta | $18.00 | 2 |
  | Combo A (Salad + Burger) | $21.60 | 2 |
  | Combo B (Salad + Pasta) | $24.30 | 3 |

  A combo costs 90% of its two dishes together (rounded half up) and takes the sum of their units.
- **Happy Hour**: once per game, lasts 2 turns. Orders taken during it get 20% off (after any VIP discount),
  and customers awaiting service regain 15 satisfaction (up to 100) at the start of each of those turns.
  Orders taken before it keep their old price.
- **Win** at the end of turn 18: at least 4 customers paid, revenue of at least $45.00, and an average satisfaction
  of paid customers of at least 60. The game always plays all 18 turns.

### Turn order

1. Arrivals join the waiting queue.
2. While Happy Hour is unused, you are asked whether to start it now. If it is active, waiting customers recover.
3. You command the Waiter (one task), then the Host (seat one customer), then the Chef (one preparation unit).
4. The Cashier collects from at most one customer who has been ready to pay since an earlier turn (oldest first).
5. Customers served on an earlier turn finish eating and become ready to pay.
6. Customers still waiting, seated or ordered lose satisfaction by their type (never below 0). At 0 they leave and their order is cancelled.
7. An active Happy Hour loses one turn. After the last turn, the result is shown.

## Controls

| Prompt | Command | Meaning |
|--------|---------|---------|
| Happy Hour | `yes` / `no` (or `y` / `n`) | Start Happy Hour now, or ask again next turn |
| Waiter | `seat <c> [t]` | Seat customer c at table t (default: lowest free table) |
| Waiter | `order <c>` | Take c's order; the customer chooses the dish |
| Waiter | `serve <c>` | Serve c's READY order |
| Host | `seat <c> [t]` | Same seating rules as the Waiter |
| Chef | `cook <c>` | Advance c's order by one unit |
| Waiter / Host / Chef | `wait` (or `w`) | Do nothing this turn |
| Any prompt | `help`, `status`, `menu` | Show help, reprint the board, show the menu (no turn used) |
| Any prompt | `quit` | Leave the game after confirming |

Ids can be written `3` or `C3`, tables `2` or `T2`; commands are not case-sensitive.
An invalid command prints why and asks again. It never uses up the staff member's turn.
If the input ends, every remaining prompt counts as `wait` (or `no`) and the game still finishes.

## How customers choose food (Rule F1)

Customer Cn orders menu item number `(n - 1) mod menuSize`. The menu lists the three dishes first, so C1 orders
Salad, C2 Burger and C3 Pasta, then C4 would order Combo A, C5 Combo B and C6 Salad. The rule is deterministic so
runs are reproducible. You see the dish when you take the order. It lives in `Customer.chooseDish(Menu)`, so a
customer type can override it: the Critic does, so C4 orders Combo B, the priciest item.

## Reproducible runs

| Script | Result |
|--------|--------|
| `scripts/stage3-victory.txt` | VICTORY: 4 paid, $64.80, average 84.5 (Happy Hour on turn 12) |
| `scripts/stage3-defeat-early-stop.txt` | DEFEAT: 2 paid, $22.50, average 87.0, 1 departure |
| `scripts/stage3-defeat-idle.txt` | DEFEAT: 0 paid, 2 unhappy departures |

Each turn in a script has a Happy Hour line (until it is used), then a Waiter, a Host and a Chef line.
The Stage 1 and 2 scripts and their results are available at git tags `stage-1` and `stage-2`.
Details and turn tables are in `docs/test-notes/`. The turn-16 customer can never pay within 18 turns. That is intended.

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
Stage1_Development_Plan.md   the plan the stages were built from
```

Dependencies only point one way: `ui` and `control` use `domain`; `domain` never imports them.
Money is stored as integer cents everywhere and formatted only for display.
