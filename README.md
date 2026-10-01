# Smart Pantry Manager

An Android app (Java) that helps you reduce food waste by tracking the ingredients you already have at home and suggesting only the recipes you can cook right now, with no shopping trip required.

Built for Mobile App Development 700, Practical Assignment.

---

## What it does

- **Pantry management** — add, edit and delete ingredients, each with a name, quantity, unit and optional expiry date.
- **Pantry list** — a RecyclerView bound to the database, with expiring-soon items highlighted.
- **20 pre-loaded recipes** — seeded into the database automatically on first run.
- **Suggested Recipes** — shows only recipes where *every* ingredient is in the pantry in at least the required quantity (the strict-matching rule).
- **Almost There** — a clearly separated bonus list of recipes missing exactly one ingredient.
- **Recipe detail** — full ingredient list marked ✓ or ✗ against your pantry, plus the method.
- **Settings** — expiring-soon alerts and how many days ahead to warn, show/hide the Almost There list, default unit for new items, and clear all pantry items.

The app uses no location, GPS or mapping features, and requests no permissions at all.

## Screens

| Screen | Class |
|---|---|
| Pantry List (launcher) | `ui/PantryListActivity` |
| Add / Edit Ingredient | `ui/AddEditItemActivity` |
| Suggested Recipes | `ui/SuggestedRecipesActivity` |
| Recipe Detail | `ui/RecipeDetailActivity` |
| Settings | `ui/SettingsActivity` |

Navigation is a BottomNavigationView across the three main screens, with explicit Intents carrying the item or recipe id to the detail screens.

---

## Database choice: SQLite

I chose **SQLite**, implemented with `SQLiteOpenHelper` in `data/DatabaseHelper.java`.

Why:

1. **The app is single-user and on-device.** The pantry belongs to one person on one phone. There is nothing to sync between users or devices, so a cloud database would add complexity without adding value.
2. **It works offline.** Recipe suggestions must work in a kitchen with no signal. SQLite is local, so there is no network dependency and no loading states to handle.
3. **The data is relational.** One recipe has many ingredients, so `recipes` and `recipe_ingredients` are separate tables joined by a foreign key. This is exactly what a relational database is for, and it keeps ingredient quantities queryable instead of buried in a text blob.
4. **No accounts or setup.** Firebase would need a Google project, config files and an internet connection to mark. PostgreSQL would need a REST backend that I would also have to host, for a single-user app that never leaves the device.
5. **It is built into Android** and matches the persistent data chapter covered in the module.

### Schema

| Table | Columns |
|---|---|
| `pantry_items` | `_id`, `name`, `quantity`, `unit`, `expiry_date` (nullable) |
| `recipes` | `_id`, `name`, `description`, `steps` |
| `recipe_ingredients` | `_id`, `recipe_id` (FK → `recipes`), `name`, `quantity`, `unit` |

Full CRUD lives in `DatabaseHelper`: `insertPantryItem` (Create), `getAllPantryItems` / `getPantryItem` (Read), `updatePantryItem` (Update), `deletePantryItem` (Delete). Data is written to a file in the app's private storage, so it persists after the app is closed and reopened.

---

## The strict-matching rule

Implemented in `logic/RecipeMatcher.java`. A recipe is suggested **only** when every ingredient it needs is in the pantry in at least the required quantity.

1. The pantry is summarised into an index: normalised ingredient name → unit category → total amount in base units.
2. Each recipe's ingredients become requirements in the same form.
3. A requirement passes only if `available >= required` within the same unit category.
4. 0 failures → **Suggested**. Exactly 1 failure → **Almost There**. 2 or more → shown nowhere.

Having an ingredient but not enough of it counts as missing.

### Handling real-world messiness

- `logic/IngredientNormalizer.java` lower-cases names, strips punctuation and describing words (`fresh`, `large`, `chopped`), singularises words (`tomatoes` → `tomato`, `berries` → `berry`), and maps synonyms (`scallions` → `spring onion`, `cake flour` → `flour`).
- `logic/UnitConverter.java` converts units within a category, so 1 kg satisfies a 500 g requirement. Units are never converted **across** categories: grams cannot satisfy millilitres, because that would require the density of each food and could wrongly claim you can cook something.

---

## Requirements

- Android Studio (latest stable)
- Android SDK 34
- A device or emulator running Android 7.0 (API 24) or newer
- JDK 17 (bundled with Android Studio)

## Setup and run

```bash
git clone https://github.com/<your-username>/SmartPantryManager.git
```

1. In Android Studio choose **File > Open** and select the cloned `SmartPantryManager` folder (the one containing `settings.gradle`).
2. Wait for the Gradle sync to finish. The first sync downloads Gradle 8.7 and the AndroidX libraries, so it needs internet.
3. Choose a device: either **Tools > Device Manager > +** to create an emulator with an API 34 image, or plug in a phone with USB debugging enabled.
4. Press **Run** (Shift+F10).

On first launch the pantry is empty and the 20 recipes are seeded into the database automatically.

### Trying it out

Add these four items: `Eggs 6 pcs`, `Milk 1 l`, `Butter 250 g`, `Salt 500 g`. Open the Recipes tab and **Scrambled Eggs** appears under "Ready to cook", with Cheese Omelette and Mashed Potatoes under "Almost there". Delete the butter and Scrambled Eggs drops out of the suggestions.

### Running the tests

```bash
./gradlew test
```

Or in Android Studio, right-click `app/src/test/java/com/example/smartpantry` and choose **Run 'Tests in smartpantry'**. The tests cover the strict-matching rule, unit conversion and name normalisation, and run on your computer with no emulator needed.

---

## Project structure

```
app/src/main/java/com/example/smartpantry/
├── model/     PantryItem, Recipe, RecipeIngredient
├── data/      DatabaseHelper (SQLite), RecipeSeedData
├── logic/     IngredientNormalizer, UnitConverter, RecipeMatcher
├── adapter/   PantryAdapter, RecipeAdapter
├── ui/        the five Activities + NavHelper
└── util/      Prefs (SharedPreferences), DateUtils
app/src/test/  RecipeMatcherTest, IngredientNormalizerTest
```

The matching logic in `logic/` contains no Android code, which is what makes it testable with plain JUnit.

## Known limitations

- The singulariser is rule-based, so unusual plurals may need adding to the exception list.
- Synonyms come from a fixed list rather than a dictionary.
- Expired items still count as being in the pantry; they are highlighted rather than excluded.

## Author

<Your name> — <student number>
