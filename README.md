# GeoMapShots

GeoMapShots is an offline-first Android app for taking photos, saving where they were captured, viewing them on Google Maps, and exporting polished photo + schematic-map collages.

## Features

- CameraX capture inside the app.
- Location metadata saved locally in Room; photos still save when location is denied or unavailable.
- Google Maps Compose map with custom thumbnail markers and photo bottom sheets.
- Gallery grid grouped by date with filters and multi-select.
- Photo detail screen with share, favorite, note, delete, and mini-map.
- Collage picker/editor with five templates: Postcard, Map story, Journey timeline, Grid + map, and Polaroid map.
- Canvas-based collage renderer that exports PNG via MediaStore to `Pictures/GeoMapShots`.
- Material 3, dynamic color, dark theme, and edge-to-edge UI.

## Tech Stack

- Kotlin, Jetpack Compose, Material 3.
- CameraX.
- Google Maps Compose and Google Maps Android SDK.
- Room, Coroutines, Flow.
- Hilt for dependency injection.
- Coil for local image loading.
- MediaStore and FileProvider for export/share.

## Run

Add a Google Maps API key to `local.properties`:

```properties
MAPS_API_KEY=your_key_here
```

Enable these Google Cloud APIs:

- Maps SDK for Android (required for the in-app map).
- Maps Static API (required to render the real Google mini-map inside collages — if disabled, the renderer falls back to a stylized schematic map).

Then build:

```bash
./gradlew assembleDebug
./gradlew test
./gradlew lintDebug
```

## Permissions

GeoMapShots requests:

- `CAMERA` to capture photos.
- `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` to mark photos on the map.
- `INTERNET` for Google Maps tiles.

The app does not request background location. It stores original photos and thumbnails in app-specific storage and exports collages through MediaStore.

## Privacy

- Coordinates are stored locally in Room.
- EXIF geotagging is off by default.
- Photos can be captured without location permission.
- Settings include precise-coordinate display and EXIF geodata toggles.
- “Delete all data” removes local Room records plus app-managed photo/thumbnail files.

## Maps Attribution

Collage export uses Google Maps Static API to render the real mini-map for the photo's location(s) with required Google attribution baked into the image. If the Static Maps API is not enabled or the request fails (offline, quota, etc.), the renderer falls back to a stylized schematic map that does not use Google tiles. We never screenshot the live Google Maps view inside the app.

## Known Limitations

- Marker clustering is not enabled yet; custom thumbnail markers are used directly.
- Reverse geocoding/address lookup is not implemented in the MVP.
- EXIF geotag writing is exposed as a setting but intentionally not written by default.
- Collage maps use Google Static Maps when the API is enabled; the renderer falls back to a stylized schematic map otherwise.
