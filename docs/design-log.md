# Design log

One entry per decision, for the report. Format: decision, alternatives, why, principles, which later stages it helps.
Dates are placeholders to be filled from the commit history.

## Layering: ui / control / domain
Decision: three packages with dependencies pointing down only; `domain` never prints or reads input.
Alternatives: one flat package; rules inside the console classes.
Why: the brief requires rules to stay out of the UI. The domain returns values (`ActionResult`, `Payment`, lists of
departed customers) and the view formats them. A search for `System.out` in `domain` finds nothing.
Principles: SRP, separation of concerns. Affects stages: all.

## Customer is abstract; behaviour varies by overriding
Decision: `Customer` is abstract with `RegularCustomer` as the concrete type. `getSatisfactionLossPerTurn()`,
`getTypeLabel()` and `chooseDish(Menu)` are the polymorphic points.
Alternatives: one concrete class with a `type` field and `switch` statements.
Why: the brief bans type checks for customer behaviour; a new customer type should be a new subclass only.
The codebase contains no `instanceof`.
Principles: OCP, LSP, polymorphism. Affects stages: 2.

## ManagerInput interface
Decision: the controller depends on `ManagerInput`, with `ConsoleInput` and `ScriptedInput` implementing it.
Alternatives: the controller reading `System.in` directly.
Why: two real implementations exist now (interactive play and reproducible script replay), and tests drive the
same controller with strings. Not an interface for its own sake.
Principles: DIP. Affects stages: 3 (Host prompt reuses it).

## ScriptedInput extends ConsoleInput
Decision: scripted replay reuses the console parsing and meta-command handling by overriding a single `readLine`.
Alternatives: a separate class with duplicated parsing.
Why: no duplicated parsing or help/quit logic; fallback to the keyboard when a script ends comes for free.
Principles: DRY, template method.

## Commands as objects (WaiterTask / ChefTask)
Decision: the parser builds `SeatTask`, `TakeOrderTask`, `ServeTask`, `CookTask` or `WaitTask`; the staff member
executes it and returns an `ActionResult`.
Alternatives: a `switch` on the command text inside the controller.
Why: the controller has no command-specific code. A failed result means the controller asks again, which is what
keeps invalid input from using up a turn.
Principles: Command pattern, SRP, OCP. Affects stages: 3 (new staff role adds its own tasks).

## Seating lives in Restaurant.seatCustomer
Decision: validation and effects of seating are in `Restaurant`; `Waiter.seat` only delegates.
Alternatives: seating logic inside `Waiter`.
Why: a later Host role also seats customers and must not copy the rules.
Principles: DRY, OCP. Affects stages: 3.

## Staff.describeAvailableTasks
Decision: each staff member describes what it could do now; the board loops over a `List<Staff>`.
Alternatives: the view working out tasks itself.
Why: the view holds no game knowledge, and a new staff member appears on the board without editing the view.
Principles: polymorphism, OCP. Affects stages: 3.

## PriceCalculator.quote is the only place a price is decided
Decision: price is computed once, when the order is taken, and stored in the `Order`.
Alternatives: computing price at payment time.
Why: promotions and discounts apply to orders when taken, and must not change orders already placed.
Principles: SRP, single extension point. Affects stages: 2 and 3.

## MenuItem immutable but not final
Decision: immutable class with private fields, left open for subclassing.
Why: a combo can later be a `MenuItem` made from other items without changing `Order` or `Menu`.
Principles: composition, LSP. Affects stages: 3.

## GameConfig, ArrivalSchedule and VictoryTargets as data
Decision: turns, tables, menu, arrivals (with a factory per arrival) and targets are injected into `Restaurant`.
Alternatives: constants inside the rules.
Why: the base setting is one factory method; tests use tiny configurations (for example a Pasta-only game to replay
the brief's example); new customer types are added by changing the arrival schedule only.
Principles: OCP, testability. Affects stages: 2.

## Victory logic uses integer maths
Decision: the average-satisfaction test is `sum >= minAverage * paidCount`; money is integer cents.
Why: no floating-point boundary errors (an average of exactly 60 wins; 59.75 does not). PASS/FAIL per target also
lives in `VictoryTargets`, so the view contains no rule.
Principles: correctness, SRP.

## State changes are package-private
Decision: methods that change customers, orders, tables and the scoreboard are package-private; collections are returned unmodifiable.
Why: the UI and controller can read state but cannot break rules. Illegal transitions throw `IllegalStateException`
because user mistakes are caught earlier by validation, so reaching one is a programming error.
Principles: encapsulation.

## Waiter acts before the Chef
Decision: the Waiter prompt is executed before the Chef prompt is shown.
Why: it makes the Chef's options include an order taken this turn, and guarantees a dish finished this turn cannot
be served this turn, with no extra flag.

## Byte-order mark handling
Decision: input strips a leading BOM.
Why: found during testing. Windows Notepad and PowerShell pipes add one, which made the first command read as
unknown and shifted a whole script. A test now covers it.

## Stage 2: VIP and Critic as subclasses, bill hook on Customer
Decision: `VIPCustomer` and `CriticCustomer` extend `Customer` and override loss rate, label, `adjustBill` (VIP) and
`chooseDish` (Critic). `PriceCalculator.quote` calls `customer.adjustBill`. The base run changes by two entries in
`ArrivalSchedule.base()`.
Alternatives: a type field with `if` chains in the controller and price code.
Why: the controller, staff and view needed no change (checked by diff), which is the point of the Stage 1 design.
Principles: OCP, LSP, polymorphism.

## Stage 2: Stage 1 scripts kept as Regular-only regression tests
Decision: the base setting now contains a VIP and a Critic, so the Stage 1 hand-traced numbers no longer hold for it.
They are kept as tests under a Regular-only test setting (`TestConfigs.regularOnly()`), and the files stay in `scripts/`.
Why: the game is data-driven, so an older setting is one config, not a code branch.

## Stage 3: Host reuses Restaurant.seatCustomer; tasks serve two roles
Decision: `Host` is a new `Staff` that calls the same seating method as the Waiter. `SeatTask` and `WaitTask`
implement `HostTask` as well as `WaiterTask`, so the parser and tasks are not duplicated. The controller gets one
more prompt, and the board lists the Host without any view change.
Alternatives: copying the seating checks into `Host`; a separate `HostSeatTask`.
Why: one place for the seating rules (DRY), and the Stage 1 prediction that Stage 3 would only add a staff role held.
Principles: DRY, OCP, polymorphism.

## Stage 3: ComboMeal is a MenuItem
Decision: `ComboMeal extends MenuItem` and is built from two items; the menu lists it after the single dishes.
Nothing that handles orders knows about combos. `MenuItem.getDescription()` lets the menu show a combo's parts
without the view checking types.
Why: `Order`, `Chef`, `Menu` and `PriceCalculator` work unchanged; keeping dishes first keeps rule F1's results
for C1 to C3.
Principles: LSP, composition, OCP.

## Stage 3: Happy Hour lives in the domain, owned by Restaurant
Decision: `HappyHour` holds its own state (unused, active, turns left). `Restaurant` owns it and applies the
recovery; `PriceCalculator.quote` applies the discount after the customer's own bill. The controller only sequences:
ask, apply recovery, end the turn.
Alternatives: a flag and arithmetic in the controller.
Why: the promotion rules are testable without the UI, and "VIP discount first, then Happy Hour" is in one method.
Principles: SRP, encapsulation.

## Stage 3: seat messages are staff-neutral
Decision: `Restaurant.seatCustomer` says "Seated C1 at T1." instead of "Waiter seats ...".
Why: two roles call it; naming one of them would be wrong half the time.

## Stage 3: earlier-stage scripts are removed from the working tree
Decision: the prompt format changed (Happy Hour question, Host line), so Stage 1 and 2 scripts no longer run.
They stay available at the `stage-1` and `stage-2` tags; their rules are covered by unit tests.
Why: scripts that fail at the current commit would mislead anyone running them.

## Rejected: one GameEngine class with a command switch
Why rejected: every later feature would mean editing it (OCP violation), and customer behaviour would need type
checks that the brief bans.

## Interfaces deliberately not added
`ITable`, `IOrder`, `IMenu`, `IView`: each would have a single implementation and no planned variation, so they
would only exist to tick a box. Revisit if a second implementation ever appears.

## Testing approach
Decision: a small dependency-free `TestRunner` instead of JUnit, so a marker can run the tests with only a JDK.
Besides unit tests there are full-game replays and a seeded random-game test that audits invariants at every prompt.
It found no rule bugs; its only effect was confirming the model. The BOM problem was found by manual testing.
