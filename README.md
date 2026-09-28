# LifeDesk

**Everything in your life that has a date, payment or deadline.**

LifeDesk is an Android app that finds, tracks and reminds you about everything you're about to forget —
bills, subscriptions, IDs, insurance, warranties, renewals, appointments and fines — and helps you finish the job.

> Take any document/photo → understand what matters → automatically create the right reminder.

## What's in v1.0

| Feature | What it does |
|---|---|
| 📸 **Just take a photo** | Camera, gallery, or *Share → LifeDesk* from WhatsApp/Gmail/Photos. On-device OCR (Google ML Kit, offline) reads the document. |
| 🧠 **Understands the document** | Detects the type (insurance, credit card, passport, Emirates ID, visa, DEWA/ADDC/e&/du bills, subscriptions, tenancy/Ejari, warranty, school fees, fines, flights…), the provider, amount & currency, the *important* date (expiry vs due vs issue vs birth date), reference/policy numbers, phone and email. Warranty end dates are computed from “2 years warranty” + purchase date. |
| 🏠 **Home dashboard** | “Good morning. You have 4 things that need attention.” 🔴 urgent / 🟠 upcoming / 🟢 monitored, money due this month, subscriptions per month, documents expiring, and per-asset cards (e.g. 🚗 Toyota — insurance 32 days, registration 61 days). |
| ✅ **Actions, not just reminders** | Mark as paid/renewed (recurring items roll forward automatically), remind me later, call/email the provider, compare renewal, plan renewal checklist (passport, Emirates ID, visa, licence), check warranty claim, upload new document. |
| 📈 **Before you pay** | When you scan a new invoice from the same provider: “This is 18% higher than last time. Previous AED 3,220 → Current AED 3,800. Difference +AED 580.” Price history per item, price-increase alerts on Home. |
| 💸 **Where is my money going?** | All subscriptions with monthly cost and *last used*; flags ones unused for 30+ days and shows the potential monthly/yearly saving. |
| 🔎 **Ask anything** | Offline natural-language search: “When does my insurance expire?”, “How much do I spend on subscriptions?”, “Show documents expiring this month”, “What payments are due this week?” |
| ⏰ **Smart reminders** | Daily check at your chosen time; nudges at 30/14/7/3/1/0 days (plus 6 and 3 months ahead for passports/IDs/visas) and for 3 days after an overdue date. Tap a notification to open the item. |
| 🔒 **Private** | No account, no server. Data and photos stay on the phone. JSON backup/restore. |
| 🎛 **Sample data** | “Explore with sample data” fills the app with a realistic UAE household so you can demo it instantly. |

UAE-first defaults (AED, day-first dates, UAE providers & documents); other GCC currencies, USD, EUR, GBP, INR and PKR are supported.

## Download

**[⬇️ Download LifeDesk.apk (latest)](https://github.com/sulemanshehzad560-cloud/Life-Desk/releases/latest/download/LifeDesk.apk)** · [All releases](https://github.com/sulemanshehzad560-cloud/Life-Desk/releases)

Open the file on your Android phone (Android 8.0+) and allow "Install unknown apps" when asked.

## Getting the APK

### Option A — GitHub Actions (no Android Studio needed)
Every push runs **Build APK** (`.github/workflows/build-apk.yml`): unit tests + `assembleDebug`.
Open the run in the **Actions** tab → download the **LifeDesk-debug-apk** artifact → unzip → install `app-debug.apk`
on your phone (allow “Install unknown apps” for your browser/file manager).

Push a tag like `v1.0.1` to publish a new GitHub Release; the download link above always points to the newest one.

### Option B — build locally
Requirements: JDK 17+, Android SDK (Android Studio Ladybug or newer).
```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # parser / search / urgency unit tests
```

### Signed release APK (for Play Store / public distribution)
```bash
keytool -genkey -v -keystore lifedesk.jks -keyalg RSA -keysize 2048 -validity 10000 -alias lifedesk
LIFEDESK_KEYSTORE=$PWD/lifedesk.jks LIFEDESK_KEYSTORE_PASSWORD=... LIFEDESK_KEY_ALIAS=lifedesk LIFEDESK_KEY_PASSWORD=... ./gradlew assembleRelease
```
In CI, add repository secrets `KEYSTORE_BASE64` (`base64 -w0 lifedesk.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
and the workflow also produces **LifeDesk-release-apk**. For Google Play, build an AAB with `./gradlew bundleRelease`.

## Tech stack

Kotlin · Jetpack Compose (Material 3) · Room · WorkManager · ML Kit Text Recognition (bundled, offline) · Navigation Compose.
minSdk 26 (Android 8.0), targetSdk 35.

```
app/src/main/java/com/lifedesk/app/
├── LifeDeskApp.kt / MainActivity.kt     app setup, share-to-LifeDesk + notification deep links
├── data/        Model.kt (items, categories, price records) · Db.kt (Room) · Repository.kt · Prefs.kt
├── domain/      DocumentParser.kt (OCR text → structured item) · QueryEngine.kt (natural-language search)
│                Attention.kt (urgency, reminders, actions) · Insights.kt (savings, price changes) · Format.kt
├── ocr/         Documents.kt (ML Kit OCR with row re-assembly, private photo storage)
├── notify/      Reminders.kt (daily WorkManager job + notifications)
└── ui/          Root.kt (navigation) · AppViewModel.kt · screens/ · components/ · theme/
```
The domain layer is plain Kotlin, so the parsing and search logic is covered by fast JVM unit tests
(`app/src/test/...`).

## Roadmap

- **V2** subscriptions from e-mail receipts · **V3** bills & statements import (PDF)
- **V4** optional bank connection → automatic recurring-payment detection
- **V5** Family plan: shared household desk (you, spouse, parents)
- **V6** “Explain the difference” on price changes · **V7** LifeDesk Agent (finds renewal quotes, prepares renewals)
- Pricing idea: Free (20 documents) · Pro $4.99/mo · Family $9.99/mo
