# Smart Pantry Manager

An Android app, written in Java, that helps reduce food waste by tracking the ingredients you already have at home and suggesting only the recipes you can cook right now, with no shopping trip required.

Built for Mobile App Development 700.

## Features

- **Pantry management** — add, edit and delete ingredients, each with a name, quantity, unit and optional expiry date.
- **Pantry list** — a RecyclerView bound to the database, with items expiring soon highlighted.
- **20 pre-loaded recipes** — seeded into the database automatically on first run.
- **Suggested Recipes** — shows only recipes where every ingredient is in the pantry in at least the required quantity.
- **Almost There** — a clearly separated list of recipes missing exactly one ingredient.
- **Recipe detail** — the full ingredient list marked against your pantry, plus the method.
- **Settings** — expiring-soon alerts and how many days ahead to warn, show or hide the Almost There list, the default unit for new items, and an option to clear the pantry.

The app uses no location, GPS or mapping features and requests no Android permissions.

## Screens

| Screen | Class |
|---|---|
| Pantry List (launcher) | `ui/PantryListActivity` |
| Add / Edit Ingredient | `ui/AddEditItemActivity` |
| Suggested Recipes | `ui/SuggestedRecipesActivity` |
| Recipe Detail | `ui/RecipeDetailActivity` |
| Settings | `ui/SettingsActivity` |

A BottomNavigationView links the three main screens. The detail screens are opened with explicit Intents carrying the item or recipe id.

## Database: SQLite

I chose SQLite, implemented with `SQLiteOpenHelper` in `data/DatabaseHelper.java`.

1. **The app is single-user and on-device.** The pantry belongs to one person on one phone, so there is nothing to sync between users or devices and no benefit to a cloud database.
2. **It works offline.** Recipe suggestions need to work in a kitchen with no signal. SQLite is local, so there is no network dependency.
3. **The data is relational.** One recipe has many ingredients, so `recipes` and `recipe_ingredients` are separate tables joined by a foreign key. This keeps ingredient quantities queryable instead of buried in a text column.
4. **No accounts or servers.** Firebase would require a Google project, config files and a connection. PostgreSQL would require a REST backend to build and host, for data that never leaves the device.

### Schema

| Table | Columns |
|---|---|
| `pantry_items` | `_id`, `name`, `quantity`, `unit`, `expiry_date` (nullable) |
| `recipes` | `_id`, `name`, `description`, `steps` |
| `recipe_ingredients` | `_id`, `recipe_id` (FK → `recipes`), `name`, `quantity`, `unit` |

Full CRUD lives in `DatabaseHelper`: insert (Create), query all and query one (Read), update (Update) and delete (Delete). Data is written to a file in the app's private storage, so it persists after the app is closed and reopened.

## How the matching works

`logic/RecipeMatcher.java` decides whether a recipe qualifies as a suggestion.

1. The pantry is summarised into an index: normalised ingredient name → unit category → total amount in base units.
2. Each recipe's ingredients are turned into requirements in the same form.
3. A requirement passes only when the available amount is at least the required amount, within the same unit category.
4. No failures means the recipe is suggested. Exactly one failure puts it in the Almost There list. Two or more and it is not shown.

Having an ingredient but not enough of it counts as missing.

Two supporting classes make this robust to real-world input:

- `logic/IngredientNormalizer.java` lower-cases names, strips punctuation and descriptive words, singularises them (`tomatoes` → `tomato`, `berries` → `berry`) and maps synonyms (`scallions` → `spring onion`, `cake flour` → `flour`).
- `logic/UnitConverter.java` converts units within a category, so 1 kg satisfies a 500 g requirement. Units are never converted across categories, because turning grams into millilitres would require the density of each food and could wrongly suggest a recipe.

## Requirements

- Android Studio (latest stable)
- Android SDK 34
- A device or emulator running Android 7.0 (API 24) or newer
- JDK 17, bundled with Android Studio

## Setup

1. Clone the repository.
2. In Android Studio choose **File > Open** and select the project folder, the one containing `settings.gradle`.
3. Wait for the Gradle sync to finish. The first sync downloads Gradle 8.7 and the AndroidX libraries, so it needs an internet connection.
4. Choose a device: either **Tools > Device Manager** to create an emulator with an API 34 image, or connect a phone with USB debugging enabled.
5. Press **Run**.

On first launch the pantry is empty and the 20 recipes are seeded into the database automatically.

## Tests

The logic classes contain no Android code, so they are covered by plain JUnit tests that run without an emulator. Right-click `app/src/test/java/com/example/smartpantry` in Android Studio and choose **Run Tests**, or run `gradlew test` from the project root.

The tests cover the strict-matching rule, insufficient quantities, unit conversion, plurals and synonyms, and the validity of all 20 seeded recipes.

## Project structure

```
app/src/main/java/com/example/smartpantry/
├── model/     PantryItem, Recipe, RecipeIngredient
├── data/      DatabaseHelper (SQLite), RecipeSeedData
├── logic/     IngredientNormalizer, UnitConverter, RecipeMatcher
├── adapter/   PantryAdapter, RecipeAdapter
├── ui/        the five Activities, NavHelper, Dialogs
└── util/      Prefs (SharedPreferences), DateUtils
app/src/test/  RecipeMatcherTest, IngredientNormalizerTest
```

## Limitations

- The singulariser is rule-based, so unusual plurals need adding to the exception list.
- Synonyms come from a fixed list rather than a dictionary.
- Expired items still count as being in the pantry; they are highlighted rather than excluded.

## Author

Graeme Croukamp - 402305796
