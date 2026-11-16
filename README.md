# UI Inspector

**UI Inspector** is a debugging overlay library for <u>Android</u> that allows developers to visually
inspect layout elements during development.

This project is inspired by the UI inspection (measurement) function of the now-<u>defunct</u> **Window VQA** app.

It is intended for **debug builds only**, and a corresponding **no-op module** ensures no overhead
or code inclusion in release builds.

| Screenshot 1                            | Screenshot 2                            | Screenshot 3                            | Screenshot 4                            |
| --------------------------------------- | :-------------------------------------- | :-------------------------------------- | :-------------------------------------- |
| <img src="./art/art_0.png" width="180"> | <img src="./art/art_1.png" width="180"> | <img src="./art/art_2.png" width="180"> | <img src="./art/art_3.png" width="180"> |

---

## Features

### Standard Inspection Mode

- When enabled, all selectable UI elements are highlighted with outlines.
- Tapping an element displays:
    - The element’s **width and height**
    - Its **distance from the immediate parent view** (top, bottom, left, right)
    - All data is shown as an overlay on the screen.

### Relative Inspection Mode

- Long-pressing a selectable element activates relative inspection mode.
- The first selected element is highlighted in **red**, and the second in **blue**.
- The distance between the two elements is shown for each side (top, bottom, left, right).
    - Sides with **zero distance are not shown**

### View System Compatibility

- Supports both:
    - Traditional `XML`-based UI trees
    - `Jetpack Compose` UI trees

---

## Compose Constraints

Since **SemanticsNode** is used for UI element extraction in **Compose**, any node that does not generate semantics during declaration will not be selectable. In such cases, make sure semantics are generated, as shown in the code below:

```kotlin
@Composable
fun Profile(profile: Profile, modifier: Modifier) {
  Box(
    modifier = Modifier
      .semantics {} // Only if the container itself needs to be selectable
      .size(100.dp)
  ) {
      Image(
        painter = painterResource(id = R.drawable.ic_profile_placeholder),
        contentDescription = "profile holder", // Option 1
        modifier = Modifier
          // or .clearAndSetSemantics {}       // Option 2
          // or .semantics {}                  // Option 3
          .fillMaxSize(),
        contentScale = ContentScale.Crop
      )
  }
}
```

---

## Permissions

This library requires the **“Draw over other apps”** permission to display the overlay UI.

First, you need to declare this permission in <u>your app</u> to allow drawing over other apps.
Add the following to your `AndroidManifest.xml` file:
(You can also place it in a <u>debug-specific manifest</u> to avoid including it in release builds.)

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest>
  <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
  ..
</manifest>
```

After installing the app, this permission must be granted before performing any UI inspection.
(Settings > Apps > Your App > Display over other apps)

---

## Additional feature

### Swipe Gesture Navigation

In measurement mode, when the target UI element is too small to select easily, swipe gesture navigation can help make selection easier. After activating the inspection feature, select any UI element. Then, by swiping in any of the four directions—up, down, left, or right—the selected element changes based on the swipe direction. Two types of view tree navigation methods are supported.

The default is the **Hierarchy** mode, which allows intuitive navigation through parent, child, and sibling nodes. For example, swiping to the right moves through sibling nodes sequentially. When the last sibling node is reached, no further movement occurs.

The other type is **DFS (Depth-First Search)** mode, which navigates the view tree using pre-order traversal. For instance, swiping to the right does not move to a sibling node but instead first traverses the child nodes of the current node, and then continues to the next sibling.

|           Hierachy (Default)            |                   DFS                   |                Demo (DFS)                 |
| :-------------------------------------: | :-------------------------------------: |:-----------------------------------------:|
| <img src="./art/art_4.png" width="270"> | <img src="./art/art_5.png" width="270"> | <img src="./art/art_m_0.gif" width="270"> |

The diagram illustrates how the tree navigation changes according to the swipe gesture direction. For example, In DFS mode, moving from Top → B can be done using either the GD or GR swipe gestures. **GD** is a shorthand for Gesture Down.

### Relative Guide Styles (NONE/EDGE/FULL)

In Relative inspection mode, you can control how guide lines are rendered via a style.

- **NONE**
  - Only distance lines, arrows, and labels are shown.
- **EDGE**
  - Short edge guides are drawn on the nearest faces of the two elements, along with distance lines and labels.
  - If the gap is too narrow, edge guides may be omitted.
- **FULL**
  - Linear guide lines visualize the relationship between the two elements.
  - When the elements are completely separated, the primary guide extends only to the nearest face of the secondary, while the secondary spans the full outer extent.
  - If there is overlap on any axis, both guides are clamped to the overlapping projection or nearest faces.
  - If one element is fully contained within the other, the inner element’s guide lines extend to the outer element’s faces.

|                   None                    |                   Edge                    |                   FULL                    |
| :---------------------------------------: | :---------------------------------------: | :---------------------------------------: |
| <img src="./art/art_m_1.gif" width="270"> | <img src="./art/art_m_2.gif" width="270"> | <img src="./art/art_m_3.gif" width="270"> |

### Details and Editing

A floating panel shows detailed information for the selected element and provides limited editing capabilities.

- **Scope**
  - Editing is currently available for the **XML-based View system** only.
  - Editing for Jetpack Compose elements is not supported.
- **Examples**
  - Layout-related properties such as margin and padding
- **Others**
  - The floating panel supports UI scale adjustment.

|                  Details                  |                  Editing                  |                 UI Scale                  |
| :---------------------------------------: | :---------------------------------------: | :---------------------------------------: |
| <img src="./art/art_m_4.gif" width="270"> | <img src="./art/art_m_5.gif" width="270"> | <img src="./art/art_m_6.gif" width="270"> |

### Settings / Toggle APIs

You can control behavior at runtime via the following APIs.

- **Relative Guide Style**
  ```kotlin
  Inspector.setRelativeGuideStyle(RelativeGuideStyle.EDGE)
  val style = Inspector.getRelativeGuideStyle()
  ```
- **Traverse Type**
  ```kotlin
  Inspector.setTraverseType(TraverseType.HIERARCHY)
  Inspector.setTraverseType(TraverseType.DFS)
  ```
- **Unit Mode**
  ```kotlin
  Inspector.setUnitMode(UnitMode.DP)
  Inspector.setUnitMode(UnitMode.PX)
  ```
- **Details Panel**
  ```kotlin
  Inspector.enableDetailsView()
  Inspector.disableDetailsView()
  Inspector.setDetailsViewUiScale(1.0f)
  ```

---

## Download

Add the following dependencies from Maven Central:

```kotlin
dependencies {
  debugImplementation("io.github.uhufor:inspector-runtime:latest.release")
  releaseImplementation("io.github.uhufor:inspector-noop:latest.release")
}
```

## Example

```kotlin
class InspectorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Install before using
        Inspector.install(this)
        // To use the default floating control button (Option)
        Inspector.showFloatingTrigger()
    }
}

// For manual control of inspection
class InspectorActivity : Activity() {
    fun enableInspection() {
        Inspector.enableInspection()
    }

    fun disableInspection() {
        Inspector.disableInspection()
    }
}
```

## Compose style inspection

The existing details panel now shows text content, text/background `#AARRGGBB` colors with alpha-aware swatches, source font size in **sp**, numeric font weight and italic state when available. Unknown values display **확인 불가**. Compose styles are read-only; View text editing and all existing measurements remain supported.

Call `Inspector.install(this)` in `Application.onCreate()`, **before the first composition**, then use the existing `Inspector.enableDetailsView()`. No per-component inspection Modifier is needed for standard Text/TextField. Runtime installation enables Compose's global `isDebugInspectorInfoEnabled` to retain Modifier metadata. The flag stays enabled for subsequent compositions; elements created before installation may need screen recreation. Keep `debugImplementation(":inspector-runtime")` and `releaseImplementation(":inspector-noop")` (using `project(...)` for local modules); the no-op module is unchanged and does not enable metadata.

This implementation targets the resolved Compose UI/Foundation **1.6.8** (BOM `2024.08.00`):

- Text uses `SemanticsActions.GetTextLayoutResult`; missing, false, throwing or ambiguous results keep only available `Text`/`EditableText` semantics content. Password semantics are never queried for unmasked layout/content.
- Overlapping AnnotatedString spans are merged into effective runs. Each property is shown only if uniform across the text. Mixed/unspecified properties, shader colors and `em` font sizes stay unknown; line height and letter spacing preserve their declared `sp`/`em`. No guessed defaults or pixel-to-sp conversion are used for Compose.
- Background uses public `layoutInfo.getModifierInfo()` / `InspectableValue` metadata for one local `Modifier.background`. A `SolidColor` brush includes its explicit alpha. Gradients, multiple backgrounds, image painters, standard custom drawing modifiers, inaccessible metadata and unsupported drawing stay unknown. There is no private-field reflection or dependency on newer background semantics APIs; other Compose versions are not guaranteed.
- Colors describe configured local paint, not composited pixels, parent colors or final graphics-layer opacity. Surface/Canvas/custom Modifier drawing may be unavailable. Arbitrary custom draw behavior cannot be reconstructed.
- Selection still uses the unmerged Semantics tree. A plain Box/Canvas without semantics is not newly selectable. Only if such a container itself needs selection, add `Modifier.semantics { }` to that container. Hidden descendants of `clearAndSetSemantics` cannot be recovered.
- View support is preserved. Gradient drawables and ripple effect colors are no longer misreported as solid background colors.

Run `:inspector-runtime:testDebugUnitTest` and `:inspector-runtime:connectedDebugAndroidTest` (API 28+ device/emulator required for instrumentation).

Validation for this change: **all 35 unit tests and all 28 instrumentation tests passed** (Pixel 9 Pro / Android 16 emulator). Coverage includes styles, guide rendering, View regression, overlay resizing/recreation, independent section toggles, selection/unit changes, Edit action separation, preference reloads that preserve panel size, and coalescing/flushing/canceling window updates. A real PROP window retained its attachment and size through 42 section toggles. Regressions also cover the first inspection toggle after late Application installation, Activity registration/cleanup, extracting layouts only for the final hit candidate, and static, manually scrollable long values. Overlay permission for the window regression test is declared only in the instrumentation APK. Physical sample drags verified `820 × 1114 → 880 × 1204 → 820 × 1114px`; a full app process restart restored the same saved size. App debug/release, runtime release and no-op release builds passed. Runtime debug lint passed with zero errors and one existing long-vector-path warning. The final debug app was installed on the emulator.

Installing with an Application context after an Activity has resumed recovers the visible Activity from existing app windows, so the first inspection toggle does not require backgrounding and resuming the app. Repeated installs with an Activity context refresh the tracked Activity without duplicate lifecycle registration. Installing before the first composition is still recommended for background metadata.

### Runtime cost

Inspection startup, refresh and overlay viewport changes scan the current View/Compose tree and retain style snapshots. Cost grows with node count and text/Modifier complexity; there is no periodic full-tree polling. Tapping an element extracts Compose styles only after the final candidate is chosen, without invoking text-layout callbacks on intermediate candidates. Disabling inspection releases the inspection overlays and scan snapshots; the Compose metadata flag remains enabled.

PROP values no longer auto-scroll. **Drag a value horizontally** to read long contents. Collapsing sections removes their contents from composition. Release builds using `inspector-noop` skip inspection processing. Functional regression tests do not establish a CPU/FPS improvement percentage, and debug/emulator timings do not represent release performance on physical devices.

API references: [GetTextLayoutResult](https://developer.android.com/reference/kotlin/androidx/compose/ui/semantics/SemanticsActions), [LayoutInfo.getModifierInfo](https://developer.android.com/reference/kotlin/androidx/compose/ui/layout/LayoutInfo). Background metadata was also checked against the resolved Foundation 1.6.8 source JAR.

### Resize the PROP panel

Hold and drag any of the **four diagonal corner handles** to resize the PROP card. The opposite corner stays fixed. Content scrolling, View editing and guide toggles continue to work inside the card.

Drag updates are coalesced to the latest size once per display frame, and identical window frames are skipped. Releasing flushes the final pending size before saving it.

The size is saved in the host app's private preferences, in dp, when a drag ends. It is restored when the panel reopens or the app process restarts. Clearing app data or uninstalling the app resets it. Default content size remains `148 × 180dp`; minimum content size is `120 × 120dp`, both adjusted for the existing UI scale. A custom size is stored separately from that text/UI scale.

Size and position stay inside the available screen. Rotation temporarily clamps a large preferred size without overwriting it, so it can return on a larger screen. Cancelled drags revert and do not save. Position still follows the existing floating-trigger anchor. No new permissions are required, and release no-op behavior is unchanged.

### Collapse PROP sections

Click the title of **Details, Distance, Margin / Padding, Text, Actions, Appearance or Alignment** to independently hide/show its contents. Titles and disclosure arrows remain available when collapsed. All sections initially start expanded; collapse choices persist across selections, units, panel reopenings and app restarts without changing the saved panel size. Details collapses only its ID group, not the remaining sections. Sections without applicable data remain absent as before.

View **Edit** actions have separate click targets and are hidden while their section is collapsed. Returning from editing preserves other sections' states. Collapsing affects the PROP display only, leaving measurements and enabled on-screen text guides unchanged.

### Design properties available in the running app

No external design/font file is required. Scroll the existing PROP details panel:

- **Text** adds configured font family, line height, letter spacing, alignment and include-font-padding. Compose preserves declared `sp`/`em`; View line height uses measured `px` and letter spacing uses `em`. Mixed/unspecified properties stay unknown. Font labels identify configured generic families or bundled resource names, not a guessed glyph fallback typeface.
- **Appearance** adds physical TL/TR/BR/BL corner radii, border width/color with a swatch and local layer alpha. Radii use measured Shape outlines and respect layout direction; generic paths and conflicting shapes stay unknown. Border width preserves declared dp, including `Hairline (1px)` for zero-dp strokes. Multiple borders and gradient border colors stay unknown.
- **Alignment** adds text layout bounds, line count and first/last baseline offsets from the text layout top. Enable **Baseline guide** for magenta lines and **Text bounds guide** for yellow layout outlines. They start off, persist across selections and are disabled when layout data is unavailable. These are layout bounds, not glyph-ink outlines; intermediate baselines are not guessed on Compose 1.6.8. Decorated inputs whose container size differs from the text layout keep typography but omit guides because the inner text origin is unavailable.
- Local layer alpha excludes ancestors and fill-color alpha. `graphicsLayer { ... }` blocks are never executed by the inspector, so unreadable block-based values remain unknown. View support includes readable drawable corners and View alpha; drawable stroke/family details and geometry for transformed Views can be unavailable.
- Compose padding/margin placeholders now display unknown instead of misleading zeroes. Existing bounds/distance measurements remain available. Values reflect the latest selection/scan; refresh after app state changes.

Select the blue text in **Compose UI → Background color samples** to compare `16sp` font size, `24sp` line height, `0.5sp` spacing, sans-serif, centered text, `8dp` corners, `2dp / #FF0D47A1` border and `90%` layer alpha.

## License

```
Copyright [2025] [uhufor (Hae Jung, Kim)]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
