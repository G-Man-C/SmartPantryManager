# Smart Pantry Manager: Step-by-Step Guide

This guide explains how the app is built, in the order you would build it yourself, and why each decision was made. Use it to understand the code well enough to explain it in your report and video.

---

## 1. Opening and running the project

1. Unzip `SmartPantryManager.zip`.
2. In Android Studio choose **File > Open** and select the `SmartPantryManager` folder (the one containing `settings.gradle`).
3. Let Gradle sync. It downloads Gradle 8.7, Android Gradle Plugin 8.5.2 and the AndroidX libraries, so the first sync needs internet.
4. Run on an emulator or phone (Android 7.0 / API 24 or newer).

If Android Studio complains about a missing Gradle wrapper, open **Settings > Build, Execution, Deployment > Build Tools > Gradle** and set "Distribution" to *Wrapper* (it will read `gradle/wrapper/gradle-wrapper.properties`). Alternatively, create a new empty "Views" project with package `com.example.smartpantry` and copy the `app/src` folder and `app/build.gradle` dependencies into it.

To run the unit tests: right-click `app/src/test/java/com/example/smartpantry` > **Run 'Tests in smartpantry'**. They run on your computer; no emulator is needed.

---

## 2. Project structure

```
app/src/main/java/com/example/smartpantry/
├── model/      PantryItem, Recipe, RecipeIngredient     plain data classes
├── data/       DatabaseHelper (SQLite), RecipeSeedData  persistence + 20 seeded recipes
├── logic/      IngredientNormalizer, UnitConverter,     the strict-matching engine
│               RecipeMatcher                            (pure Java, unit-tested)
├── adapter/    PantryAdapter, RecipeAdapter             custom RecyclerView adapters
├── ui/         PantryListActivity, AddEditItemActivity, the 5 screens
│               SuggestedRecipesActivity, RecipeDetailActivity,
│               SettingsActivity, NavHelper
└── util/       Prefs (SharedPreferences), DateUtils
app/src/test/   RecipeMatcherTest, IngredientNormalizerTest
```

The key design idea is **separation of concerns**: the matching logic in `logic/` has no Android code at all. That means it can be tested with ordinary JUnit tests, and the screens only have to display results.

---

## 3. Building it step by step

### Step 1: Model classes (`model/`)
`PantryItem` (id, name, quantity, unit, optional expiry date), `Recipe` (id, name, description, steps, list of ingredients) and `RecipeIngredient` (name, quantity, unit). These are simple objects with getters and setters that move data between the database, the logic and the screens.

The expiry date is stored as text in `yyyy-MM-dd` format. It sorts correctly as text, has no time-zone problems, and is null when the user doesn't set one.

### Step 2: The SQLite database (`data/DatabaseHelper.java`)
`DatabaseHelper` extends `SQLiteOpenHelper`, the approach from the module's persistent data chapter. It creates three tables:

| Table | Purpose |
|---|---|
| `pantry_items` | The user's ingredients: full CRUD |
| `recipes` | Seeded recipes (name, description, steps) |
| `recipe_ingredients` | One row per ingredient per recipe, with a foreign key to `recipes` |

Recipes and their ingredients are in separate tables because one recipe has many ingredients (a one-to-many relationship). Putting ingredients in one text column would make them impossible to query properly.

The CRUD methods are `insertPantryItem` (Create), `getAllPantryItems` / `getPantryItem` (Read), `updatePantryItem` (Update) and `deletePantryItem` (Delete). All of them use `ContentValues` and `?` placeholders rather than building SQL strings from user input, which prevents SQL injection.

The helper is a **singleton** (`getInstance`) so the whole app shares one database connection.

**Persistence:** SQLite writes to a file in the app's private storage, so data survives closing the app, swiping it away, and restarting the phone. It is only removed if the app is uninstalled or its data is cleared.

### Step 3: Seeding recipes on first run (`data/RecipeSeedData.java`)
`onCreate()` in `SQLiteOpenHelper` runs **only once**, the first time the database file is created. That makes it the correct place to insert the 20 recipes, and they are never duplicated. The recipes live in their own class so they're easy to edit, and a unit test checks that every recipe has ingredients, steps and valid units.

### Step 4: Handling messy ingredient names (`logic/IngredientNormalizer.java`)
The brief says a naive exact-string match that breaks on "tomato" vs "tomatoes" will be marked down. Every name, from both the pantry and the recipes, goes through the same pipeline:

1. Lower-case it and turn punctuation into spaces (`"Extra-virgin"` becomes `"extra virgin"`).
2. Strip measure phrases (`"cloves of garlic"` becomes `"garlic"`).
3. Remove describing words that don't change the ingredient (`fresh, large, chopped, frozen…`).
4. Singularise each word with rules (`berries` → `berry`, `tomatoes` → `tomato`, `peaches` → `peach`, `eggs` → `egg`), with exceptions for words like `couscous` and `hummus`.
5. Map synonyms to one name (`scallion` / `green onion` → `spring onion`, `cake flour` → `flour`, `mielie meal` → `maize meal`, `baby marrow` → `zucchini`).

Two names match if they produce the same key. This is deliberately **not** full NLP, which the brief says isn't needed. It is a small, predictable, testable rule set.

### Step 5: Handling unit differences (`logic/UnitConverter.java`)
Every unit belongs to a **category**, and each category has a base unit:

| Category | Units (factor to base) | Base |
|---|---|---|
| Mass | g (1), kg (1000) | g |
| Volume | ml (1), l (1000), tsp (5), tbsp (15), cup (250) | ml |
| Count | pcs (1) | pcs |

So 1 kg of flour in the pantry satisfies a recipe needing 500 g. Quantities are **never compared across categories**. Grams can't satisfy millilitres, because that would need the density of every food and could produce a false "you can cook this". When in doubt, the strict rule says don't suggest.

### Step 6: The strict-matching rule (`logic/RecipeMatcher.java`)
This is the most important class in the assignment.

1. **Build a pantry index:** normalised name → unit category → total amount in base units. Two entries "Eggs 4 pcs" and "egg 2 pcs" add up to 6 pcs of `egg`.
2. **Build the recipe's requirements** the same way.
3. **Check every requirement:** it is satisfied only if `available >= required` in the same category (with a tiny tolerance for floating-point rounding).
4. **Classify the recipe:**
   - 0 unsatisfied: **Suggested**
   - exactly 1 unsatisfied: **Almost There** (bonus list, kept separate)
   - 2 or more: shown nowhere

Having an ingredient but **not enough of it** counts as missing, which is what "in at least the required quantity" in the brief means. The Almost There list explains the gap, for example "Need 10 g more Butter" or "Missing: Cheese (40 g)".

### Step 7: Custom adapters (`adapter/`)
`PantryAdapter` and `RecipeAdapter` extend `RecyclerView.Adapter` with a `ViewHolder` that caches the views in each row. Clicks are passed back to the Activity through a small `Listener` interface, so the adapter doesn't need to know about Intents or the database.

`PantryAdapter` also colours expiry text: orange when an item expires soon, red when it has expired, controlled by the settings. `RecipeAdapter` is reused by both recipe lists; Almost There rows show an extra note.

### Step 8: The screens (`ui/`)

| Screen | What it does |
|---|---|
| `PantryListActivity` (launcher) | RecyclerView of pantry items, + button, tap a row to edit, bin icon to delete (with confirmation), empty-state message, expiring-soon banner |
| `AddEditItemActivity` | One form for both **Create** and **Update**. If the Intent contains `EXTRA_ITEM_ID` it's editing; otherwise adding. Also offers Delete when editing |
| `SuggestedRecipesActivity` | Runs `RecipeMatcher` and shows two separate sections: "Ready to cook" and "Almost there". Shows a friendly message when nothing matches |
| `RecipeDetailActivity` | Full ingredient list with ✓ / ✗ per ingredient, plus numbered steps |
| `SettingsActivity` | Expiring-soon alerts on/off, warning window in days, show/hide Almost There, default unit for new items, clear pantry |

Lists reload in `onResume()`, so when you add an ingredient and press back, the pantry and suggestions update immediately.

### Step 9: Navigation and Intents
- **Bottom navigation bar** on the three main screens (Pantry, Recipes, Settings), set up once in `NavHelper`. Each tab starts its Activity with an explicit Intent and `FLAG_ACTIVITY_REORDER_TO_FRONT`, so repeatedly switching tabs doesn't pile up copies of screens.
- **Passing data with Intents:** the pantry list sends the item id to the edit screen (`putExtra(EXTRA_ITEM_ID, id)`), and the suggestions list sends the recipe id to the detail screen. Only the id is passed; the receiving screen loads fresh data from the database, so it's never out of date.
- **Up/back:** the edit and detail screens have an Up arrow (`parentActivityName` in the manifest).

### Step 10: Input validation (`AddEditItemActivity.validate()`)
All fields are checked at once and errors are shown on the fields themselves using `TextInputLayout.setError`:

- **Name:** required, 40 characters maximum (a live counter is shown), letters/numbers/spaces/hyphens/apostrophes only, and it must contain at least one letter.
- **Quantity:** required, must be a valid number, greater than 0, and not absurdly large. A comma is accepted as the decimal point, since many South African phone keyboards produce `0,5`.
- **Unit:** chosen from a Spinner, so it can't be invalid.
- **Expiry date:** chosen from a DatePicker, so there's no typing errors. New items can't be given a past date.

The database also enforces `quantity > 0` with a `CHECK` constraint, giving a second layer of protection.

### Step 11: Settings (`util/Prefs.java`)
Settings are saved with **SharedPreferences**, which is the right tool for small key-value options. SQLite holds the structured data (pantry and recipes); SharedPreferences holds preferences. Each change is saved immediately with `apply()`.

---

## 4. Requirements checklist

| Requirement (brief) | Where it's met |
|---|---|
| Java only, Android Studio | Whole project; no Kotlin files |
| ≥ 4 screens | 5 Activities (see Step 8) |
| Intents to navigate and pass data | `EXTRA_ITEM_ID`, `EXTRA_RECIPE_ID`, `NavHelper` |
| RecyclerView + custom Adapter bound to DB | `PantryAdapter`, `RecipeAdapter` |
| Navigation element | BottomNavigationView + Up arrows |
| Input validation | `AddEditItemActivity.validate()` |
| Suitable layouts | ConstraintLayout, LinearLayout, ScrollView, CardView |
| Database: full CRUD + persistence | `DatabaseHelper` (SQLite) |
| 15–20 seeded recipes | `RecipeSeedData` (20), seeded in `onCreate()` |
| Strict matching | `RecipeMatcher` + 13 unit tests |
| Robust to plurals / units | `IngredientNormalizer`, `UnitConverter` |
| Zero-match feedback | `tvEmptySuggested` message |
| Settings screen | `SettingsActivity` |
| Bonus: Almost There, clearly separated | Separate section, divider, own heading and colour |
| No Maps / GPS / location | No location permissions in `AndroidManifest.xml`; no maps libraries |

---

## 5. Demo script for your video

This sequence shows every marked feature in a few minutes. Start from a fresh install (or use Settings > Clear all pantry items).

1. **Empty states:** open the Recipes tab and point out the "Your pantry is empty…" message.
2. **Create and validation:** tap +, press Save with an empty form to show the errors, and type `-5` as the quantity. Then add:
   - `Eggs`, 6, pcs
   - `Milk`, 1, l
   - `Butter`, 250, g
   - `Salt`, 500, g
3. **Strict match:** open Recipes. **Scrambled Eggs** is ready to cook. Point out that 1 l of milk satisfies 50 ml (unit conversion).
4. **Almost There:** Cheese Omelette appears only in the separate section with "Missing: Cheese (40 g)", and Mashed Potatoes with "Missing: Potatoes". Pancakes do **not** appear anywhere because they are missing two ingredients.
5. **Synonyms:** add `Cheddar`, 200, g. Cheese Omelette moves up to Ready to cook.
6. **Quantity check and Update:** add `Potatoes`, 3, pcs. Mashed Potatoes stays in Almost There with "Need 2 pcs more Potatoes". Edit the potatoes to 5 and it moves to Ready to cook.
7. **Plurals:** show that the recipe says "Eggs" or "Potatoes" while your entry could be "egg" or "potato", and it still matches.
8. **Detail screen:** open a recipe to show the ✓ / ✗ ingredient list and the numbered method.
9. **Delete:** delete Butter. Scrambled Eggs disappears from suggestions.
10. **Persistence:** close the app completely (swipe it away from recents), reopen it, and show the pantry is still there.
11. **Settings:** add an item expiring tomorrow to show the orange highlight and banner, then toggle alerts off and back on. Toggle the Almost There list off.
12. **Unit tests:** run `RecipeMatcherTest` in Android Studio and show all tests passing, especially `fourOfFiveIngredientsIsNotSuggested`, which is the exact scenario from the brief.

---

## 6. Points for your report

- **Why SQLite:** it works offline, needs no account or server, is built into Android, and is covered in the module. The data is relational (recipes have many ingredients), which suits SQL tables with a foreign key. Firebase would add network dependency and setup for no benefit in a single-user, on-device app.
- **Why the matcher is separate from the UI:** it can be tested without an emulator, and the suggestions screen and the detail screen use the same code, so they can never disagree.
- **Why units aren't converted across categories:** converting grams to cups needs a density per food. Guessing would break the "genuinely has everything" guarantee, so the app refuses to guess.
- **Known limitations (honest to mention):**
  - The singulariser is rule-based, so unusual plurals may need adding to the exception list.
  - Synonyms are a fixed list.
  - Expired items still count as "in the pantry" (they are highlighted instead).
- **Possible future work:**
  - A "Cook this" button that subtracts ingredients from the pantry.
  - Letting users add their own recipes.
  - Excluding expired items from matching as an option.
