# iOS capture provenance

These PNG files are actual native Simulator screenshots produced by [GitHub Actions run 38417654835](https://github.com/mohamedkbay/fakkerni/actions/runs/38417654835), source commit `cf864bc095a0a408df52799c74434c3397af5f27`.

- `phone-*` and `tablet-*`: Arabic interface, dark/light appearance; `phone-large-text` uses the accessibility-large content size.
- `assistant-*`, `editor-*`, `settings-*`, `sounds-*`, `reminders-*`, and `connect-*`: English UI-test captures with synthetic reminders and no real contacts or credentials.
- `provider-browser-return`: the app-owned return action while the external provider page is still loading. It is return-flow evidence, not a screenshot of the provider's finished website.
- `screen-tour.gif`: a slideshow of seven real PNG captures, 3 seconds per screen. It does not demonstrate live assistant animation or record a real AI interaction.

Animations are paused in DEBUG-only UI-test fixtures for repeatable screenshots; release behavior remains animated and respects Reduce Motion. PNG metadata records the capture origin. The GIF derives from these PNGs without generated imagery.
