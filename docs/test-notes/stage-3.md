# Stage 3 test notes (Host, Combo Meals, Happy Hour)

Commit: `<fill in at tag time>`   Tag: `stage-3`   JDK: 21 (compiled with `--release 17`)   OS: Windows 11

## What changed from Stage 2

| Change | Where |
|--------|-------|
| `Host` staff member: seats at most one queued customer per turn, using the same `Restaurant.seatCustomer` as the Waiter | new `Host`, `HostTask` |
| `SeatTask` and `WaitTask` also work as Host tasks | small change |
| `ComboMeal extends MenuItem`: two dishes sold together, units add up, price is 90% of the two prices (half up) | new file |
| Base menu gains Combo A (Salad + Burger, $21.60, 2 units) and Combo B (Salad + Pasta, $24.30, 3 units) after the three dishes | `Menu.base()` |
| `HappyHour`: once per game, lasts 2 turns, 20% off new orders (after any VIP discount), +15 satisfaction (max 100) to customers awaiting service at the start of each active turn | new `HappyHour`, `SatisfactionChange`; owned by `Restaurant` |
| `PriceCalculator.quote` also takes the Happy Hour: VIP discount first, then Happy Hour | small change |
| Turn start gets a Happy Hour phase between arrivals and the board; Host prompt sits between Waiter and Chef | `GameController` |
| `ManagerInput` gains `nextHostTask` and `nextHappyHourDecision` | `ManagerInput`, `ConsoleInput` |
| Seat messages are staff-neutral ("Seated C1 at T1.") because two roles use them | `Restaurant` |

`Cashier`, `Chef`, `Order`, `Table`, the customer classes and `VictoryTargets` are unchanged apart from a one-line
`Customer.gainSatisfaction`.

## Turn format (affects scripts)

Each turn asks, in this order: **Happy Hour yes/no** (only while it is still unused), **Waiter**, **Host**, **Chef**.
After Happy Hour is used the first question disappears. Scripts therefore have four lines per turn until
activation, then three.

## Decisions made where the plan was open (check against the assignment brief)

- Happy Hour recovery applies at the start of **each** of its two active turns, including the turn it starts.
- It is offered every turn until used; there is no way to ask for it mid-turn.
- Host acts after the Waiter and before the Chef, and the Waiter can still seat customers too.
- Combos sit after the single dishes, so rule F1 (customer n orders item (n-1) mod menu size) gives C1 to C3
  the same dishes as before. C4 would get Combo A and C5 Combo B; C4 is the Critic, who takes the priciest item,
  Combo B.

## How to reproduce

```
build.bat
run.bat scripts\stage3-victory.txt
run.bat scripts\stage3-defeat-early-stop.txt
run.bat scripts\stage3-defeat-idle.txt
test.bat
```

The Stage 1 and Stage 2 scripts are no longer in the working tree because the prompt format changed. They are
available at tags `stage-1` and `stage-2`, and their rules are still covered by unit tests.

## Winnability

An exhaustive search over every sensible command sequence (the search is a throwaway tool, not part of the repository)
finds winning lines whether Happy Hour is never used (441 distinct winning end states) or started on turns 6, 9 or 12
(680, 716 and 757). So Happy Hour helps but is not required, and the best lines need real choices: this setting is
tighter than Stage 2 (4 paid is usually the most that fits).

## Run 1 - Victory (`scripts/stage3-victory.txt`, Happy Hour on turn 12)

| Turn | Event |
|------|-------|
| 1-3 | C1: seated, orders Salad $9.00, served |
| 4 | C2 (VIP) arrives, seated by the Waiter |
| 5-6 | C2 orders Burger at **$13.50**; C1 pays $9.00 on turn 5 |
| 7-9 | C3 seated, orders Pasta, cooked in two turns; C2 pays $13.50 on turn 8 |
| 10-11 | C4 (Critic) arrives, seated, orders **Combo B $24.30** (3 units); Chef cooks 1/3 |
| 12 | Answer **yes** to Happy Hour: C3 60 -> 75 and C4 76 -> 91; Chef cooks 2/3 |
| 13 | C5 arrives; recovery again (C3 67 -> 82, C4 79 -> 94); Waiter serves C3; Chef finishes Combo B |
| 14 | Waiter serves C4 |
| 15-16 | C3 pays $18.00 (sat 82); C4 pays $24.30 (sat 82); C5 is seated by the Host on turn 16 |
| 17-18 | C6 seated by the Host; C5 orders Combo B (cannot finish in time) |

Final: 4 paid, $64.80, average satisfaction 84.5, 0 departures -> **VICTORY**.
Without Happy Hour the Critic would have paid at a satisfaction near 52 and C3 near 60.

## Run 2 - Defeat, early stop (`scripts/stage3-defeat-early-stop.txt`)

Turns 1-10 as in Run 1, then everyone waits and Happy Hour is declined every turn.
Final: 2 paid, $22.50, average 87.0; the unattended Critic C4 walks out. 1 departure -> **DEFEAT**.

## Run 3 - Defeat, idle (`scripts/stage3-defeat-idle.txt`)

All staff wait for 18 turns and Happy Hour is declined every turn.
Final: 0 paid, $0.00, average N/A, 2 departures (C1 at the end of turn 13, the Critic C4 at the end of turn 18).
The VIP C2 is still waiting at 25 and C3 at 4.

## Automated checks added

- **Host:** seats a queued customer, same refusals and hints as the Waiter (unknown customer, already seated,
  occupied table with free-table hint), board lists the Host's tasks, nothing offered when tables are full, can wait.
- **Combo:** price 90% of the pair (Combo A $21.60, Combo B $24.30), units add up, rounds half up, description lists
  the parts, rule F1 over five items, Critic picks the priciest combo, a three-unit combo needs three cooks.
- **Happy Hour state:** available once; lasts exactly two turns; cannot be activated twice or after it ended;
  remaining turns never go negative; the Restaurant refuses a second activation with a clear message.
- **Happy Hour pricing:** Regular 20% off; VIP then Happy Hour ($15.00 -> $13.50 -> $10.80); combos; orders taken
  before it keep their old price; revenue uses the price locked at order time.
- **Happy Hour recovery:** +15 capped at 100, only for customers awaiting service (not eating, not paid, not gone),
  none while inactive, record of before and after values.
- **Parser:** Host commands and errors (wrong-role messages mention what the Host can do), yes/no answers.
- **Scenario:** the three scripts above; Happy Hour asked and declined on turns 1-11 and never again after use;
  board shows 2 then 1 turn left and nothing afterwards; invalid input at every prompt including Happy Hour; end of
  input counts as "no"; quit and help at every prompt; menu lists combos with their parts; full tables refused for
  the Waiter and the Host; leaving customer with a pending order; byte-order mark.
- **Random-game audit** (400 seeded games) now includes the Host and random Happy Hour decisions, and checks that
  each locked price is either the customer's bill or that bill with the Happy Hour discount.

## Known intended behaviour

- The turn-16 customer cannot pay within 18 turns.
- A typo inside a script file uses up one script line, so later lines shift by one.
- Answering the Happy Hour question in a script uses a line each turn until it is used.
