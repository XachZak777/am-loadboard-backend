**Add your own guidelines here**
## FE ↔ Existing BE comparability contract

This section helps Backend (BE) quickly decide whether the **current Frontend (FE)** data model can be **compared/mapped** to the **existing BE** model without ambiguity.

### What “comparable” means
FE and BE are **comparable** when:

1. They represent the **same domain entity** (e.g., a “Load” posting).
2. There is a **1:1 field mapping** (or a documented transform) for all fields FE uses.
3. The mapping is **deterministic** (no guessing from free-text).
4. BE can preserve FE behavior for:
   - load listing & filtering
   - load detail
   - booking request flow

If any critical FE field has no BE equivalent and can’t be derived reliably, treat as **NOT comparable** until this contract is updated.

---

### FE canonical data shapes (source of truth)

#### `Load` (FE: `src/app/types.ts`)

Required:
- `id: string`
- `vehicleType: string` (FE values: `sedan|suv|truck|van|motorcycle|rv|boat|atv`)
- `make: string`
- `model: string`
- `year: number`
- `pickupCity: string`
- `pickupState: string` (2-letter)
- `deliveryCity: string`
- `deliveryState: string`
- `pickupDate: string` (date-only `YYYY-MM-DD`)
- `deliveryDate: string` (date-only `YYYY-MM-DD`)
- `price: number` (USD)
- `distance: number` (miles)
- `condition: 'running' | 'non-running'`
- `isOpen: boolean`
- `contactName: string`
- `contactEmail: string`

Optional:
- `brokerId?: string`
- `brokerEmail?: string`
- `notes?: string`

FE behavior depending on these fields:
- `src/app/pages/LoadBoard.tsx` filters by text search over `make/model/pickupCity/deliveryCity/pickupState/deliveryState`, by `vehicleType`, by `condition`, by “state filter” matching pickup OR delivery state, and only shows loads where `isOpen === true`.
- `src/app/pages/LoadDetail.tsx` shows details/contact and creates booking requests.

#### `LoadBooking` (FE: `src/app/types/user.ts`)

- `id: string`
- `loadId: string`
- `carrierId: string`
- `carrierName: string`
- `carrierEmail: string`
- `status: 'available'|'requested'|'booked'|'picked-up'|'in-transit'|'delivered'|'paid'`
- `requestedAt: string` (ISO timestamp)
- `bookedAt? / pickedUpAt? / deliveredAt? / paidAt?: string`
- `notes?: string`

---

### Minimum BE fields required (to support current FE)

BE must provide a `Load` equivalent with at least:
- identity: `id`
- vehicle: `vehicleType`, `make`, `model`, `year`, `condition`
- lane: `pickupCity`, `pickupState`, `deliveryCity`, `deliveryState`
- dates: `pickupDate`, `deliveryDate`
- pricing: `price`, `distance`
- availability: `isOpen` (or a status mappable to `isOpen`)
- contacts: `contactName`, `contactEmail`
- broker metadata: `brokerId`, `brokerEmail` (optional but used for notifications)
- notes: `notes` (optional)

> Note: Phone numbers are **not required** by the current auth/registration workflow. If phone fields appear in FE designs,
> they must be treated as optional UI-only fields unless/until BE adds first-class support.

If BE stores contact info differently (e.g., nested under broker/company), BE must either:
- return the flattened fields FE expects, **or**
- FE must be updated with an adapter (requires FE change).

---

### Normalization rules (for deterministic comparisons)

Before comparing FE vs BE payloads:
- Strings: `trim()`, collapse whitespace, compare case-insensitively for city/state/make/model.
- States: normalize to 2-letter uppercase.
- Dates: FE is date-only; if BE uses timestamps, compare by date component (unless BE introduces windows).
- Price: numeric USD (not `"$450"`).
- Distance: numeric miles.
- Enums: 
  - `condition` must map exactly to `running | non-running`.
  - `vehicleType` must map into FE accepted values.

---

### Field mapping table (BE fills in)

| FE field | FE type | BE field/path | Transform | Notes |
|---|---:|---|---|---|
| `id` | string |  | none | |
| `vehicleType` | string |  | enum map | must match FE values |
| `make` | string |  | none | |
| `model` | string |  | none | |
| `year` | number |  | number parse | |
| `pickupCity` | string |  | trim/casefold | |
| `pickupState` | string |  | uppercase | |
| `deliveryCity` | string |  | trim/casefold | |
| `deliveryState` | string |  | uppercase | |
| `pickupDate` | string |  | timestamp→date | compare by date |
| `deliveryDate` | string |  | timestamp→date | compare by date |
| `price` | number |  | currency parse | USD |
| `distance` | number |  | number parse | miles |
| `condition` | enum |  | enum map | `running/non-running` |
| `isOpen` | boolean |  | status→boolean | |
| `contactName` | string |  | none | |
| `contactEmail` | string |  | lowercase | |
| `brokerId` | string? |  | none | optional |
| `brokerEmail` | string? |  | lowercase | optional |
| `notes` | string? |  | none | optional |

---

### Pass/Fail definition
**Comparable = PASS** if, after normalization, BE can provide all required FE `Load` fields and preserve `isOpen` semantics (only open loads appear on the board).

**NOT comparable = FAIL** if BE lacks lane granularity (pickup/delivery city+state), has ambiguous pickup dates (only windows without a mapping), or `price` means something else without a defined conversion.


System Guidelines

Use this file to provide the AI with rules and guidelines you want it to follow.
This template outlines a few examples of things you can add. You can add your own sections and format it to suit your needs

TIP: More context isn't always better. It can confuse the LLM. Try and add the most important rules you need

# General guidelines

Any general rules you want the AI to follow.
For example:

* Only use absolute positioning when necessary. Opt for responsive and well structured layouts that use flexbox and grid by default
* Refactor code as you go to keep code clean
* Keep file sizes small and put helper functions and components in their own files.

--------------

# Design system guidelines
Rules for how the AI should make generations look like your company's design system

Additionally, if you select a design system to use in the prompt box, you can reference
your design system's components, tokens, variables and components.
For example:

* Use a base font-size of 14px
* Date formats should always be in the format “Jun 10”
* The bottom toolbar should only ever have a maximum of 4 items
* Never use the floating action button with the bottom toolbar
* Chips should always come in sets of 3 or more
* Don't use a dropdown if there are 2 or fewer options

You can also create sub sections and add more specific details
For example:


## Button
The Button component is a fundamental interactive element in our design system, designed to trigger actions or navigate
users through the application. It provides visual feedback and clear affordances to enhance user experience.

### Usage
Buttons should be used for important actions that users need to take, such as form submissions, confirming choices,
or initiating processes. They communicate interactivity and should have clear, action-oriented labels.

### Variants
* Primary Button
  * Purpose : Used for the main action in a section or page
  * Visual Style : Bold, filled with the primary brand color
  * Usage : One primary button per section to guide users toward the most important action
* Secondary Button
  * Purpose : Used for alternative or supporting actions
  * Visual Style : Outlined with the primary color, transparent background
  * Usage : Can appear alongside a primary button for less important actions
* Tertiary Button
  * Purpose : Used for the least important actions
  * Visual Style : Text-only with no border, using primary color
  * Usage : For actions that should be available but not emphasized

