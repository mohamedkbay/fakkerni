---
name: Fakkerni iOS
description: A bilingual native reminder assistant with Cairo type and a silver wire sculpture.
colors:
  background-dark: "#050607"
  background-light: "#F7F8F5"
  surface-dark: "#1C2021"
  surface-light: "#FFFFFF"
  accent-dark: "#E7ECE9"
  accent-light: "#202625"
  inverse-dark: "#000000"
  inverse-light: "#FFFFFF"
  secondary-dark: "#AAB1AD"
  secondary-light: "#5D6661"
typography:
  display:
    fontFamily: Cairo-Regular
    fontSize: "30pt"
    fontWeight: 600
  title:
    fontFamily: Cairo-Regular
    fontSize: "27pt"
    fontWeight: 600
  headline:
    fontFamily: Cairo-Regular
    fontSize: "18pt"
    fontWeight: 600
  body:
    fontFamily: Cairo-Regular
    fontSize: "17pt"
    fontWeight: 400
  subheadline:
    fontFamily: Cairo-Regular
    fontSize: "14pt"
    fontWeight: 400
  caption:
    fontFamily: Cairo-Regular
    fontSize: "13pt"
    fontWeight: 400
  metadata:
    fontFamily: Cairo-Regular
    fontSize: "12pt"
    fontWeight: 400
  provider-label:
    fontFamily: Cairo-Regular
    fontSize: "11pt"
    fontWeight: 400
spacing:
  compact: "4pt"
  icon-label: "8pt"
  control-gap: "12pt"
  assistant-stack: "18pt"
  section: "20pt"
  page: "24pt"
components:
  button-primary-dark:
    backgroundColor: "{colors.accent-dark}"
    textColor: "{colors.inverse-dark}"
  button-primary-light:
    backgroundColor: "{colors.accent-light}"
    textColor: "{colors.inverse-light}"
  surface-dark:
    backgroundColor: "{colors.surface-dark}"
  surface-light:
    backgroundColor: "{colors.surface-light}"
---

# Design System: Fakkerni iOS

## Overview

**Creative North Star: "The Silver Assistant"**

Fakkerni preserves the established Cairo, monochrome, silver-orb and Morphicons identity in a native SwiftUI interface. The wire sculpture provides the expressive center; quiet surfaces and readable Arabic and English support the reminder work around it.

This is the implemented iOS system, extracted from `ios/App/Visuals.swift`, `FakkerniApp.swift`, `ReminderScreens.swift`, and `SettingsScreens.swift`. The approved identity is recorded in `PRODUCT.md`; surface composition belongs in `docs/ios-surface.md`. The finish review reports ship across all 14 supplied captures, with no material fixes in publication scope. Native dimensions here are SwiftUI points, not CSS pixels. System controls retain their platform behavior.

**Key Characteristics:**

- Cairo across Arabic and English, with Western digits.
- Near-black and near-white grounds with neutral action contrast.
- A silver wire sculpture and the existing vector mark.
- Native navigation, grouped forms, sheets and accessible controls.

## Colors

The palette is near-neutral, with a slight green cast in several implemented neutrals.

### Primary

`accent-dark` and `accent-light` supply the app tint and filled primary controls; the corresponding `inverse` token supplies their content. Appearance suffixes identify the active interface theme, not a lightness scale.

### Neutral

`background` is the full-screen ground, `surface` is the raised tonal field for grouped rows and small controls, and `secondary` is supporting text. SwiftUI `.primary` remains the platform's semantic foreground where the implementation uses it; it is not replaced by a fabricated fixed color token.

**The Neutral Identity Rule.** Keep brand surfaces and ordinary actions monochrome. Native destructive and validation states retain their semantic treatment.

The source uses system red for failures/destructive actions and green for the completion swipe action. These are local semantic states, not additional brand accents. The orb's Canvas rendering owns its translucent silver strokes; it is not a new surface palette.

## Typography

**Display Font:** Cairo-Regular with SwiftUI weight modifiers.

**Body Font:** Cairo-Regular, inherited from the app root. No explicit fallback family is authored.

The frontmatter records base sizes before Dynamic Type scaling. Display maps to `.largeTitle`, assistant title to `.title`, reminder headline to `.headline`, body to `.body`, and supporting roles to `.subheadline`, `.caption`, or `.caption2`. The primary button applies semibold to the body font. Native navigation, pickers and browser chrome retain system-managed typography where applicable.

**The Bilingual Voice Rule.** Use Cairo for authored Arabic and English content, preserve Western digits, and follow the selected layout direction.

Arabic uses `ar_LY@numbers=latn` and right-to-left layout; English uses `en_GB` and left-to-right layout. Telephone numbers, provider hosts, keys and the recording timer explicitly remain left-to-right. No fixed line-height or tracking system is authored. Wrapping and Dynamic Type are part of the system, not exceptions.

## Layout

Use native `NavigationStack`, scrollable content and focused sheets. The assistant column caps at 560pt and uses page spacing horizontally; onboarding also uses a 560pt content cap. Onboarding and connection-completion actions cap at 520pt. Other forms and inset-grouped lists use native container sizing rather than an invented breakpoint grid.

The assistant orb is 300pt high in the ordinary layout and 160pt at accessibility text sizes; onboarding uses 260pt. The bottom recording controls live in a safe-area inset. Their row has a 28pt gap, with supporting copy allowed to wrap. Editor and key-verification actions use 20pt inset padding. Toolbar action targets are 44pt; contact rows and the app-owned browser return action use a 48pt minimum height.

The compact provider capsule limits its Dynamic Type range to `.xxxLarge`; other authored Cairo text uses relative styles. Do not generalize that local limit to the whole app.

## Elevation & Depth

Authored app surfaces use tonal layering rather than custom shadows. Native sheets, grouped forms, pickers, alerts and Safari provide their own platform depth. The wire sculpture gains depth from overlapping translucent strokes, not a drop shadow or a raster image. No custom shadow token is defined.

**The Tonal Surface Rule.** Use background and surface roles to distinguish app-owned containers; preserve native presentation depth for system controls.

## Shapes

The primary action and provider entry are SwiftUI `Capsule` shapes. Recording and secondary capture controls are circles. Grouped forms, segmented pickers and rounded-border onboarding input use native shapes; their radii are not authored constants and are deliberately absent from the token frontmatter.

Morphicons are drawn from the bundled `morphicons.json` frame data with round caps and joins on a 24-unit source grid. The existing brand mark is a vector `Path`. Preserve those assets and constructions instead of replacing them with emoji or invented symbols.

## Components

### Primary button

A full-width capsule with a 54pt minimum height, semibold body text and inverse-on-accent contrast. Pressing changes the background opacity to 0.75; disabled state changes the whole control opacity to 0.45. Save and verification buttons replace or accompany text with a native progress indicator while work is pending. No custom hover or focus treatment is authored.

### Secondary controls and day shortcuts

Keyboard and reminders flank the circular recording action on surface-colored circles. Today and Tomorrow are native bordered buttons at large control size. They are date shortcuts, not a bespoke chip system. Appearance, language and reminder-history choices use native pickers; selected states remain platform-owned.

### Grouped rows and reminder cards

Settings and editor sections apply the surface role to native rows. Reminder rows use a leading circular icon field, semibold title, optional contact, divider and secondary date/status line, with 12pt vertical padding. Inset grouping, swipe actions and context menus remain native; no standalone custom card radius or shadow is defined.

### Inputs

The onboarding name field uses `.roundedBorder`; editor fields live in a `Form`. The reminder title grows from two to five lines. Phone entry uses the phone keyboard, and provider keys use a privacy-sensitive `SecureField` with native paste support. Focus and keyboard behavior remain native. Validation appears as readable text, and invalid or pending actions disable the primary button.

### Navigation and provider entry

The assistant uses Morphicons toolbar actions and a capsule containing the brand mark, assistant name and provider status. Focused flows use sheet navigation with cancellation and completion actions. The provider website opens in `SFSafariViewController` beneath an app-owned return header; returning advances to key entry, and verification is required before a connected state appears.

### Silver assistant and recording control

`AssistantOrb` draws 44 overlapping bands in a `Canvas`. Recording changes its radius with input amplitude; processing accelerates its phase. The microphone action uses an 82pt circle, a 30pt Morphicon, and a thin accent ring outside the circle. Its states are microphone, stop and spinning loader. Hold/release is complemented by a VoiceOver toggle action. Decorative artwork is hidden from accessibility.

Reduce Motion pauses the orb and disables icon transition/spin playback. The orb also pauses when the scene is inactive. The exact motion behavior is recorded in the sidecar; no web simulation is supplied for these native components.

## Do's and Don'ts

### Do:

- Do preserve Cairo, Western digits and the selected Arabic or English layout direction.
- Do use native scrolling, forms, sheets and safe-area actions with Dynamic Type.
- Do reuse the actual Morphicons data, vector mark and Canvas silver sculpture.
- Do show validation and pending states before allowing approval or provider activation.

### Don't:

- Don't introduce emoji or bright yellow UI.
- Don't invent fixed corner radii, CSS breakpoints or shadows for native controls.
- Don't turn semantic error or completion colors into brand accents.
- Don't use an account-site visit as the visual proof of a verified connection.
