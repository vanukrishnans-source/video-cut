# Video Cut v1.0

Offline Android app to **trim**, **crop**, and **merge** video segments into one MP4.

- Package: `com.vanu.videocut`
- minSdk 26 / targetSdk 34
- Kotlin + Jetpack Compose + Media3 Transformer

## How to use

1. Tap **Add video(s)** and pick one or more clips from the gallery.
2. Drag the **large amber handles** on the timeline (or type Start/End as `mm:ss.ms`).
3. Tap **Add Segment** — repeat to mark more ranges from the same video (or switch to another video and add more).
4. Reorder segments with ▲/▼; delete with the trash icon; preview with ▶.
5. Choose a **Crop** preset (16:9, 9:16, 1:1, 4:3) or drag the crop rectangle.
6. Tap **Export merged MP4** — progress shows %; Cancel works mid-export.
7. Output is saved to **Movies/VideoCut** in the gallery (H.264 + AAC).

## Build

```bash
./gradlew :app:assembleRelease
```

Signed APK lands under `app/build/outputs/apk/release/`.
