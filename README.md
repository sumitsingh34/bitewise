# BiteWise

A simple, personal calorie and nutrition tracker for meals, exercise, water and weight.
No accounts, no ads, no AI. Just see what you eat.

BiteWise is a web app that installs on an Android phone like a normal app. Everything you log is stored on your own phone.

## Features

**Food**
- Scan a product's barcode with the phone camera (data from Open Food Facts)
- Search plain foods like rice, carrot or lentils (data from USDA FoodData Central)
- Enter foods by hand, or quick-add just the calories
- My foods with a Recent list, so repeat foods take one tap
- Home recipes: build a dish from its ingredients once, then log it by the serving
- Copy yesterday's meal in one tap
- Fix wrong nutrition values, edit or remove any logged item (with confirmation)

**Nutrition**
- Calories, protein, carbs, fat, fiber, sugar, saturated fat and sodium
- Daily targets for protein, carbs and fat from your calorie goal and chosen % split
- Breakdown screens showing where calories come from, for each meal and the whole day
- Optional: exercise calories added to the day's calorie budget and macro targets

**Exercise, water and weight**
- Log exercise by activity and minutes, with calories estimated from your weight
- Water tracker in glasses (250 ml each)
- Weight log in kg or lb, with a trend chart

**History**
- Calories chart as bars or a line, for 7 days up to 3 years or all time
- Weekly and long-term averages for calories, protein, carbs and fat
- Calendar to jump to any day, with dots on days you logged food

**Other**
- Light and dark mode
- Works offline (barcode lookup and search need internet)
- Export and import backups

## Using the app

Open the app's GitHub Pages address in Chrome on your phone:

```
https://sumitsingh34.github.io/bitewise/
```

Then tap **⋮ → Add to Home screen** (or **Install app**). BiteWise opens from your home screen like any other app.

The first time you scan a barcode, allow camera access when Chrome asks.

## Files

| File | What it is |
|---|---|
| `index.html` | The whole app: layout, styles and code in one file |
| `sw.js` | Service worker, lets the app open offline and pick up updates |
| `manifest.json` | App name, colors and icons, so it can be installed |
| `icon-192.png`, `icon-512.png` | Home screen icons |

There is no build step and nothing to install. The app runs directly from these files.

## Updating the app

1. Replace the changed files in this repository and commit.
2. Wait a few minutes for GitHub Pages to update.
3. Close BiteWise on the phone and open it again, once or twice.
4. Check **Settings** (bottom of the screen) for the version number.

To see a new home screen icon, remove BiteWise from the home screen and add it again from Chrome. Logged data is not affected.

## Your data

- All logs, foods, recipes and settings are stored only in Chrome on your phone. Nothing is uploaded to this repository or anywhere else.
- The only things sent over the internet are barcode numbers (to Open Food Facts) and search words (to USDA FoodData Central).
- Use **Settings → Export backup** regularly. Clearing Chrome's site data for this address deletes everything logged.
- An optional USDA API key can be added in Settings for more searches per hour. It is saved on the phone only, never in this repository.

## Data sources

- **Open Food Facts** (openfoodfacts.org): barcode product data, a free database anyone can edit, so some products may have errors. Use **Fix nutrition values** in the app to correct them.
- **USDA FoodData Central** (fdc.nal.usda.gov): nutrition data for plain foods, per 100 g.

## Disclaimer

BiteWise is a personal tracking tool, not medical or nutrition advice. Calorie and exercise numbers are estimates. For personal targets, especially with a health condition, talk to a doctor or registered dietitian.
