# beerDeal Roadmap — Top 10 Features

The `core` engine already parses shelf tags (OCR text → price / pack / volume),
fuzzy-matches beers against a local ABV database, and ranks deals by
**cents per ounce of alcohol**. These are the top 10 features to build on it,
in priority order:

1. **Camera scan capture** — CameraX + ML Kit text recognition of shelf tags,
   feeding `TagParser` / `DealEngine` directly.
2. **Ranked deals screen** — sorted list with `centsPerAlcoholOz` as the
   headline metric, `centsPerOz` secondary, and a match-confidence badge.
   *(Shipped as the first cut of the `:app` module — manual tag entry for now.)*
3. **Manual review / correction queue** — surface tags where `toCandidate()`
   returns null or confidence is low, and let the user fill the gaps instead of
   silently dropping them.
4. **Editable local beer database** — search/add/edit ABV, brewery, and OCR
   aliases; ship a seeded dataset with room to sync updates later.
5. **Store/session grouping** — bucket scans by store visit so the same beer's
   price can be compared across locations.
6. **Barcode/UPC fallback matching** — higher-confidence alternative to fuzzy
   name matching for packaged beer.
7. **Shopping picks list** — select winning deals into a running list with
   pack counts and a budget total.
8. **Filters & preferences** — exclude 0% ABV (guards the divide-by-zero in
   `centsPerAlcoholOz`), plus pack-size / style / budget filters.
9. **Price history & deal alerts** — track a store's prices over time and flag
   new personal-best deals.
10. **Share/export results** — send a ranked deal list (text or image) to
    friends.
