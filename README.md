# Docs — დოკუმენტების წამკითხველი და რედაქტორი

უფასო, ოფლაინ Android აპი დოკუმენტების წასაკითხად და რედაქტირებისთვის.
რეკლამა არაა, ანგარიში არაა, ინტერნეტი არაა საჭირო.

## რას აკეთებს (v1)

- **მთავარი ეკრანი** — ლამაზი ფაილების სია: ფაილის გახსნა, ბოლო ფაილები, ძებნა, ფილტრი (PDF / Office / ტექსტი).
- **PDF** — გვერდების ნახვა, გადიდება/დაპატარავება, გვერდის მაჩვენებელი („3 / 12").
- **Word (.docx, .doc)** — პარაგრაფებისა და ცხრილების წაკითხვა.
- **Excel (.xlsx, .xls)** — ფურცლები ცხრილის სახით.
- **PowerPoint (.pptx)** — სლაიდების ტექსტი.
- **ტექსტი (.txt, .md, .csv)** — წაკითხვა; **.txt და .md რედაქტირდება** და იგივე ფაილში ინახება.
- ინტერფეისი ქართულად (ნაგულისხმევი), ინგლისურად და რუსულად.

## APK-ის აგება

GitHub Actions აგებს debug APK-ს ყოველ push-ზე `main` ბრენჩზე:

1. რეპოს **Actions** ჩანართი → **Build debug APK** → ბოლო გაშვება
2. **Artifacts** → `doc-reader-debug-apk` → `app-debug.apk`
3. ტელეფონზე დაყენება — უცნობი წყაროდან დაყენების ნებართვა უნდა მიეცეს

ლოკალურად: `./gradlew assembleDebug` (JDK 17).

## გამოყენებული უფასო ბიბლიოთეკები

- [Apache POI 5.2.5](https://poi.apache.org/) (Apache-2.0) — Word / Excel / PowerPoint-ის წაკითხვა
- Jetpack Compose + Material 3 — ინტერფეისი
- AndroidX DataStore — ბოლო ფაილების სია
- `PdfRenderer` (Android API) — PDF-ის ნახვა

APK-ის ზომა POI-ის გამო ~10–15 MB-ით მეტია, ვიდრე ჩვეულებრივი აპის.

---

# Docs — Document Reader & Editor

Free, offline Android app for reading and editing documents.
No ads, no accounts, no internet needed.

## What it does (v1)

- **Home screen** — beautiful file list: open a file, recent files, search, filter (PDF / Office / Text).
- **PDF** — page viewer with zoom buttons and a page indicator ("3 / 12").
- **Word (.docx, .doc)** — paragraphs and tables.
- **Excel (.xlsx, .xls)** — sheets as tables.
- **PowerPoint (.pptx)** — slide text.
- **Text (.txt, .md, .csv)** — reading; **.txt and .md are editable** and save back to the same file.
- UI in Georgian (default), English and Russian.

## Building the APK

GitHub Actions builds the debug APK on every push to `main`:

1. Repo **Actions** tab → **Build debug APK** → latest run
2. **Artifacts** → `doc-reader-debug-apk` → `app-debug.apk`
3. Install on the phone (allow installs from unknown sources)

Locally: `./gradlew assembleDebug` (JDK 17).

## Free libraries used

- [Apache POI 5.2.5](https://poi.apache.org/) (Apache-2.0) — Word / Excel / PowerPoint reading
- Jetpack Compose + Material 3 — UI
- AndroidX DataStore — recent files list
- `PdfRenderer` (Android API) — PDF viewing

The APK is ~10–15 MB bigger than a plain app because of POI.
