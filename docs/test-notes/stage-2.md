# Stage 2 test notes (VIP and Critic customers)

Commit: `<fill in at tag time>`   Tag: `stage-2`   JDK: 21 (compiled with `--release 17`)   OS: Windows 11

## What changed from Stage 1

| Change | Where |
|--------|-------|
| `VIPCustomer`: loses 5 satisfaction per turn, bill is 10% off | new file |
| `CriticCustomer`: loses 12 per turn, always orders the highest-priced dish | new file |
| `Customer.adjustBill(baseCents)` hook (Regular returns the base price) | small change |
| `PriceCalculator.quote` now calls `customer.adjustBill(...)` | small change |
| `Money.percentOf` / `Money.discounted`, rounded half up | small addition |
| Base run: the turn-4 arrival (C2) is a VIP and the turn-10 arrival (C4) is a Critic | `ArrivalSchedule.base()` |
| Banner no longer says "Stage 1" | `ConsoleView` |

`GameController`, `Waiter`, `Chef`, `Cashier` and the board code needed no changes. Nothing outside the domain
customer classes checks which kind of customer it is dealing with.

## How to reproduce

```
build.bat
run.bat scripts\stage2-victory.txt
run.bat scripts\stage2-defeat-early-stop.txt
run.bat scripts\stage2-defeat-idle.txt
test.bat
```

The `stage1-*` scripts reproduce the Stage 1 numbers at tag `stage-1`. At this stage the tests replay them under a
Regular-only test setting (`TestConfigs.regularOnly()`), because the base run now contains a VIP and a Critic.

## Why C2 and C4

An exhaustive search over every sensible command sequence shows the game is winnable wherever the VIP and Critic
are placed. VIP on turn 4 and Critic on turn 10 was chosen because C4 would order Salad under rule F1 but the Critic
orders Pasta, so the different dish rule is visible, and the Critic's fast satisfaction loss (12 per turn) forces a
real choice about whom to serve first.

## Run 1 - Victory (`scripts/stage2-victory.txt`)

| Turn | Event |
|------|-------|
| 1 | C1 arrives; Waiter seats C1 at T1 |
| 2 | Waiter takes C1's order (Salad $9.00); Chef finishes it |
| 3 | Waiter serves C1 |
| 4 | C2 (VIP) arrives; seated at T2 |
| 5 | C2 orders Burger at the VIP price **$13.50**; Chef finishes it; C1 ready to pay |
| 6 | Cashier collects $9.00 from C1 (sat 84); Waiter serves C2 |
| 7 | C3 arrives; seated at T1 |
| 8 | C3 orders Pasta $18.00; Chef cooks 1/2; Cashier collects $13.50 from C2 (sat 90) |
| 9 | Chef finishes C3's Pasta |
| 10 | C4 (Critic) arrives; Waiter serves C3 |
| 11 | Waiter seats C4 at T2 |
| 12 | C4 orders **Pasta** (priciest dish); Chef cooks 1/2; Cashier collects $18.00 from C3 (sat 76) |
| 13 | C5 arrives and is seated; Chef finishes C4's Pasta |
| 14 | Waiter serves C4 |
| 15 | C5 orders Burger $15.00; Chef finishes it |
| 16 | C6 arrives; Waiter serves C5; Cashier collects $18.00 from C4 (sat 52) |
| 17 | Waiter seats C6 |
| 18 | C6 orders Pasta; Cashier collects $15.00 from C5 (sat 76) |

Final: 5 paid, $73.50, average satisfaction 75.6, 0 departures -> **VICTORY**.
C4 pays with satisfaction 52, below 60 on its own, but the average over paid customers is what counts.

## Run 2 - Defeat, early stop (`scripts/stage2-defeat-early-stop.txt`)

Turns 1-10 as in Run 1, then both staff wait for turns 11-18.
Final: 3 paid, $40.50, average 83.3; C4 (Critic) was never seated and walks out; 1 departure -> **DEFEAT**
(paid count and revenue fail although satisfaction passes).

## Run 3 - Defeat, idle (`scripts/stage2-defeat-idle.txt`)

Both staff wait on all 18 turns.
Final: 0 paid, $0.00, average N/A, 2 departures -> **DEFEAT**. C1 (Regular) leaves at the end of turn 13 and the
Critic C4 at the end of turn 18. The VIP C2 is still waiting at satisfaction 25 (5 per turn is much slower).

## Automated checks added

- Rounding: 10% of 905 is 91 (half up), of 904 is 90, of 5 cents is 1; 0% changes nothing.
- VIP: loses 5, bill is 10% off the dish (salad $8.10, burger $13.50, pasta $16.20), order message and Cashier
  payment and revenue all use the discounted price, rule F1 still applies to the VIP.
- Critic: loses 12, picks the priciest dish, tie goes to the first listed item, works on a one-item menu,
  full price, floors at 0.
- Base run: six arrivals, exactly one VIP (turn 4) and one Critic (turn 10).
- The three Stage 2 scripts through the real controller (numbers above), including the turn on which each
  walk-out happens.
- The random-game audit now checks that each locked price equals the customer's own bill rule for that dish.
- The Stage 1 scripts still pass under the Regular-only test setting.

## Known intended behaviour

- The turn-16 customer cannot pay within 18 turns.
- A typo in a script file uses up one script line.
