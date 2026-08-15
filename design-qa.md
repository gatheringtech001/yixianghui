# Design QA

**Findings**

- No actionable P0/P1/P2 visual or interaction findings remain in the requested scope.
- [P3] The implementation uses an existing photographic travel asset while the reference uses a custom senior-travel illustration. This is intentional: the implementation preserves the 逸享荟 brand and does not copy 三意旅居 artwork.
- Intentional difference: the reference product's brand, copy and colors are not copied; only its information hierarchy and concise authorization structure are used.

**Source Visual Truth**

- Login, unchecked: `outputs/ui-comparison-20260814/screenshots/sanyi/04-login-gate.png`
- Login, checked: `outputs/ui-comparison-20260814/screenshots/sanyi/05-login-agreement-checked.png`
- Profile authorization: `outputs/ui-comparison-20260814/screenshots/sanyi/18-profile-permission.png`
- Phone authorization: `outputs/ui-comparison-20260814/screenshots/sanyi/45-phone-authorization.png`
- Existing list and calendar defects: `outputs/ui-comparison-20260814/screenshots/yixianghui/02-travel-list.png` and `outputs/ui-comparison-20260814/screenshots/yixianghui/08-calendar-confirmed.png`
- Original comparison report: `outputs/ui-comparison-20260814/test-report.html`

**Implementation Screenshots**

- `outputs/ui-implementation-20260814/screenshots/01-signin-compact-cards.png`
- `outputs/ui-implementation-20260814/screenshots/02-login-fullscreen-unchecked.png`
- `outputs/ui-implementation-20260814/screenshots/03-login-fullscreen-checked.png`
- `outputs/ui-implementation-20260814/screenshots/04-profile-fullscreen.png`
- `outputs/ui-implementation-20260814/screenshots/05-phone-fullscreen.png`
- `outputs/ui-implementation-20260814/screenshots/06-travel-list-compact.png`
- `outputs/ui-implementation-20260814/screenshots/07-calendar-dynamic-price.png`
- `outputs/auth-ux-20260814/profile-screen.png`
- `outputs/auth-ux-20260814/phone-screen.png`
- Current profile-alignment implementation screenshot: unavailable. WeChat DevTools reports the active route as `pages/my/my`, but its native capture remains on the previous settings-page GPU frame, so that capture is not valid comparison evidence.

**Viewport and Normalization**

- Source captures: 414 x 780 physical pixels, source WeChat viewport.
- Original implementation captures: 390 x 844 physical pixels and 390 x 844 CSS viewport, WeChat DevTools simulator, device scale factor 1.
- Latest profile and phone crops: 282 x 608 pixels, captured from a scaled iPhone 12/13 DevTools simulator.
- Original comparisons scale source captures to 390 px width and pad them to 390 x 844; implementation captures remain 390 x 844.
- Latest combined canvases are 828 x 780 pixels. They normalize both sides to equal-height panels for composition review; numeric pixel-perfect comparison is not claimed for these scaled captures.
- The earlier 逸享荟 list/calendar screenshots include surrounding DevTools chrome and are used only as before-state evidence.
- Current source crop: `/var/folders/wx/3l_zd3ts1vz6qmtjdjdbbp7c0000gn/T/codex-clipboard-a0bc4861-4168-42a1-b64f-f5d67f67134b.png`, 225 x 104 physical pixels.
- Current implementation viewport: iPhone 12/13 (Pro) simulator at 93% scale. A same-state implementation crop could not be captured, so no density normalization or pixel comparison is claimed for this iteration.

**State and Interaction Coverage**

- Full-page login gate opened from the sign-in page.
- Agreement unchecked and checked states rendered.
- The first CTA now performs WeChat login instead of adding a navigation-only click.
- A user with complete stored profile data exits after login without seeing avatar and nickname again.
- A user with missing profile data advances to avatar and nickname, then automatically advances when both are ready.
- Phone authorization remains optional through the visible `暂不授权` action.
- Travel list loaded from the local production snapshot and rendered compact summaries.
- Travel detail opened SKU 20 and rendered the calendar with dynamic `￥212/人` pricing.
- Sign-in page rendered seven compact day-reward cards.
- WeChat DevTools automation recorded no runtime exceptions in the successful capture run.

**Full-view Comparison Evidence**

- Login unchecked: `outputs/ui-implementation-20260814/comparisons/01-login-unchecked-reference-vs-implementation.png`
- Login checked: `outputs/ui-implementation-20260814/comparisons/02-login-checked-reference-vs-implementation.png`
- Original phone authorization: `outputs/ui-implementation-20260814/comparisons/03-phone-reference-vs-implementation.png`
- Travel list before/after: `outputs/ui-implementation-20260814/comparisons/04-travel-list-before-vs-after.png`
- Calendar before/after: `outputs/ui-implementation-20260814/comparisons/05-calendar-before-vs-after.png`
- Revised profile authorization: `outputs/auth-ux-20260814/profile-comparison.png`
- Revised phone authorization: `outputs/auth-ux-20260814/phone-comparison.png`

**Focused Region Comparison**

- Avatar action: the earlier two-character corner label was clipped. The revised original-size crop shows a complete circular camera icon with a white outline and no clipping.
- Phone copy: the revised crop contains only `逸享荟`, `康养旅居 · 活动 · 老年教育`, `授权手机号`, and `暂不授权`; the unwanted housekeeper-contact and order-notification claims are absent.
- Other authorization controls, list summaries, day cards and calendar prices remain legible in the full-view comparisons, so no additional crop is required.
- Current profile identity alignment: blocked. The source crop is readable, but the implementation capture is not in the same route/state even though the active WebView and page-path metadata both identify `pages/my/my`.

**Required Fidelity Surfaces**

- Fonts and typography: system Chinese fonts remain consistent with the mini-program; title, body, agreement and button hierarchy is clear with no clipping or unintended wrapping.
- Spacing and layout rhythm: authorization occupies the full viewport; centered brand blocks and bottom action groups follow the reference structure. Persistent controls remain visible. List and sign-in cards are materially shorter.
- Colors and visual tokens: 逸享荟 burgundy `#701018`, paper white and warm-neutral surfaces replace the reference colors intentionally and remain consistent with the existing app.
- Image quality and asset fidelity: existing 逸享荟 raster assets and the bundled uView camera icon are used; no copied competitor asset, placeholder, emoji, handcrafted SVG or CSS illustration is present.
- Copy and content: the profile page explains the two required WeChat confirmations; the phone page removes the unwanted service-contact rationale while keeping authorization and skip actions clear.
- Current identity adjustment: typography, colors, avatar asset and copy are unchanged in source and compiled output. Only `.profile-info` top padding changes from `24rpx` to `36rpx`; visual spacing remains unapproved until a fresh same-state capture is available.

**Comparison History**

1. Earlier comparison pass found two P0 issues: a hard-coded `￥196/人` calendar label and overlong travel listing content. It also showed the existing centered profile popup did not match the requested full-page authorization rhythm.
2. Fixes applied: dynamic SKU-derived unit pricing, compact cleaned listing summaries with a 220rpx travel image, 168rpx sign-in day cards, and a three-step full-page authorization flow with agreement gating and optional phone binding.
3. Post-fix evidence: the original five comparison images plus seven implementation screenshots. The successful runtime capture reported `calendarPrice: ￥212/人` and zero recorded exceptions.
4. Follow-up [P1] findings: avatar corner copy was clipped, the phone screen contained unwanted service explanations, and login required avoidable profile-flow clicks.
5. Follow-up fixes: changed the avatar action to a visible camera icon; reduced phone copy to the concise 三意-style brand structure; made the first CTA perform login; skipped complete profiles; automatically advanced incomplete profiles after avatar and nickname were ready.
6. Follow-up evidence: `profile-comparison.png`, `phone-comparison.png`, scoped source assertions, a successful HBuilderX build, and a generated WeChat development preview.
7. Current [P2] finding: the user-provided profile crop shows the login/name block still sitting too high relative to the avatar.
8. Current fix: increased `.profile-info` top padding from `24rpx` to `36rpx`, moving the identity block down by `12rpx` while leaving the avatar and surrounding layout unchanged.
9. Current post-fix evidence: the scoped source test passes, the HBuilderX build succeeds, and compiled `pages/my/my.wxss` contains `padding: 36rpx 0 2rpx`. Visual comparison remains blocked because the DevTools native screenshot is stale and mini-program screenshot automation does not return.

**Implementation Checklist**

- [x] Full-page authorization flow
- [x] Agreement unchecked/checked states
- [x] First CTA performs login
- [x] Complete profiles skip repeated avatar and nickname prompts
- [x] Unclipped avatar camera action
- [x] Automatic profile-to-phone transition
- [x] Concise optional phone authorization
- [x] Dynamic calendar unit price
- [x] Compact travel list summary
- [x] Compact sign-in day cards
- [x] WeChat DevTools build and rendered screenshot pass
- [x] Production-API development preview generated and QR decoded successfully
- [x] Profile identity source and compiled padding updated to `36rpx`
- [ ] Fresh same-state screenshot of the adjusted profile identity block

**Follow-up Polish**

- [P3] Re-capture the revised profile and phone screens at a 1:1 simulator scale if future work requires numeric spacing comparison.
- If a future brand illustration is commissioned for 逸享荟, replace the existing photographic login hero without changing the current layout or interaction contract.

final result: blocked
