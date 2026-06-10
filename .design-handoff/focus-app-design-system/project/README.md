# Focus App — Design System

> A behavior-driven focus environment for Android. Not a timer dashboard — a reactive space that breathes, glows, and responds to your discipline.

---

## Product Context

**Focus App** (working name: *FocusApp*) is an Android focus-timer app with distraction detection, analytics, and gamification. It targets a wide audience — kids, teenagers, young professionals — and trades the corporate "productivity dashboard" look for something that feels emotional, immersive, and premium.

The product's central metaphor is the **Focus Field**: instead of a dashboard, the user enters a breathing, ambient environment built around a glowing **Focus Orb** that reacts in real time to session state and distraction events.

### Surfaces

This is a **single-product** design system — one Android app, V1.

| Surface | Status | Notes |
|---|---|---|
| Android app (Jetpack Compose, Material 3) | V1 | Bottom nav with 4 tabs: Timer, Analytics, History, Profile. Plus 3-step Onboarding. |

### Three visual identities (modes)

The same core UI ships in three skins. The user picks one (or it adapts to context):

1. **Focus Mode** *(default)* — dark premium gradients, glowing orb, glass surfaces. Calm, immersive, futuristic.
2. **Chill Mode** — softer gradients, lighter colors, organic motion. Friendly, encouraging.
3. **Hardcore Mode** — monochrome, high contrast, ultra minimal. Intense, disciplined.

The design tokens in this system are tuned for **Focus Mode** (the default). Chill and Hardcore are theme variants that swap the gradient layer, accent saturation, and motion timing — see `colors_and_type.css` for the variant blocks.

---

## Sources

| Source | Type | Reference |
|---|---|---|
| `Focus App V1` brief | Pasted product spec | The aesthetic / philosophy brief: "Focus Field," orb metaphor, distraction reactions, three modes. |
| `Focus Timer App` brief | Pasted UI brief | The concrete Android spec: navigation, screens, design tokens (#6C63FF, #FFB347, #0F0F1A …), wheel picker, badge list. |

> ⚠️ **No codebase, Figma, or screenshots were attached.** Logos, illustrations, and exact iconography do not exist yet in source — they are *inferred from the brief* and flagged as such where used. If you have any of these, please attach them so I can replace the placeholders.

---

## Index

Files in this system, top down:

- **`README.md`** — this file. Product context, content fundamentals, visual foundations, iconography.
- **`SKILL.md`** — agent skill manifest. Lets this folder be used as a portable design skill in Claude Code or similar.
- **`colors_and_type.css`** — all color, type, spacing, radii, shadow, and motion tokens as CSS custom properties. Includes Chill / Hardcore theme variants.
- **`fonts/`** — webfont files. *Inter* is used for all UI text. (Loaded from Google Fonts CDN; no local TTF needed for V1.)
- **`assets/`** — logos, brand mark, sample badge artwork, illustrations.
- **`preview/`** — small HTML cards rendered in the Design System tab. Each card showcases one sub-concept (color scale, type ramp, button cluster, orb states, etc).
- **`ui_kits/android/`** — high-fidelity Android UI kit. JSX components for the timer, analytics, history, profile, onboarding. `index.html` is an interactive click-thru of the full app.

No `slides/` folder — no slide template was provided.

---

## Content Fundamentals

How copy is written across the app.

### Voice

- **Calm, encouraging, second-person.** "You broke focus after 6 min" not "User broke focus."
- **Direct, never preachy.** The app celebrates wins and acknowledges slip-ups without judgement.
- **Emotionally aware but not saccharine.** A failed session ends with "Session interrupted" — not "Don't worry, you'll do better!"
- **Confident minimalism.** Short sentences. Punchy verbs. No marketing fluff.

### Tone by surface

| Surface | Tone | Example |
|---|---|---|
| Idle / pre-session | Inviting, low-pressure | "Pick a mode. Set a duration. Start." |
| Active session | Quiet, almost no text | Just the timer. Distraction banners only when needed. |
| Distraction event | Warm, slightly playful | "You broke focus after 6 min 😅" |
| Session complete | Celebratory, brief | "Session complete! 🎉" + XP earned |
| Failed / interrupted | Soft, never harsh | "Session interrupted" — no exclamation, no shame |
| Empty states | Forward-looking | "No sessions yet — start your first focus session" |
| Permissions | Plainspoken | "Let us detect distractions" → one sentence why |

### Casing

- **Sentence case everywhere.** Buttons, labels, headers, dialogs. *"Start break"* not *"Start Break"*.
- **Exception: tab labels and brand wordmark.** Tabs are short single words ("Timer", "History") and use Title Case.
- **Numbers always numerals.** "7-day streak" not "seven-day streak". "25 min" not "twenty-five minutes".

### Pronouns

- **"You" and "your".** Never "user" or "the user". Never first-person plural except in permission flows ("We need Usage Stats permission…") where it's about the app being honest with the user.

### Emoji usage

Emoji is used **sparingly and with intent** — one per moment, never decorative.

| Use | Emoji | Where |
|---|---|---|
| Streak | 🔥 | Profile streak counter only |
| Session complete | 🎉 | Completion dialog only |
| Clean session | 🏆 | "No distractions! Clean session 🏆" empty state in session detail |
| Distraction warning | 😅 | Distraction banner only — softens the call-out |

That's it. **No emoji in nav, no emoji in buttons, no emoji in chip labels, no emoji in headings.** Badges use real artwork, not emoji.

### Numbers, units, time

- Time displays: `MM:SS` for under an hour (`24:38`), `HH:MM:SS` rare. Idle shows `00:00`.
- Durations in copy: `25 min`, `1h 30m`, `3h 20m total`.
- Dates in lists: `May 5 · 10:30 AM` — middot separator, no comma.
- Stats: `Level 4 — 1,240 XP`, `🔥 7`, `2h / 3h goal today`.

### Specific copy examples

| Where | Copy |
|---|---|
| Onboarding step 1 tagline | "Stay focused. Beat distractions." |
| Onboarding step 2 title | "Let us detect distractions" |
| Onboarding step 2 body | "We need Usage Stats permission to know when you switch apps during a session." |
| Distraction banner | "You broke focus after 6 min 😅" |
| Session complete dialog | "Session complete! 🎉" |
| Best Focus Time insight | "You focus best between 10 AM – 12 PM" |
| Tag summary | "DSA — 4 sessions · 3h 20m total" |
| Empty session detail | "No distractions! Clean session 🏆" |
| Empty analytics | "No sessions yet" / "Start your first focus session" |
| Tag input placeholder | "e.g. DSA, Project X" |

---

## Visual Foundations

The full token set lives in `colors_and_type.css`. This section explains the *why*.

### Colors

**Palette spine (Focus Mode default):**

- `--bg` `#0F0F1A` — very dark navy. The Focus Field background.
- `--surface` `#1A1A2E` — dark card / sheet. One step up from bg.
- `--surface-2` `#23233D` — secondary surface (hovered card, pressed chip).
- `--primary` `#6C63FF` — Focus purple. Used for the orb glow, primary buttons, active chips, ring progress.
- `--accent` `#FFB347` — warm amber. XP, streaks, achievement moments. *Never* used for primary actions — it's reserved for reward/celebration.
- `--success` `#4CAF50` — green. Clean sessions, break state pulse, focus score ≥ 80.
- `--warning` `#FF6B6B` — soft red. Distraction count, focus score < 50, distraction banner.
- `--text-1` `#FFFFFF` / `--text-2` `#9E9E9E` / `--text-3` `#5C5C70` (derived).

**Why this palette works:** the navy + purple set the immersive "deep space" mood the brief calls for. Amber is the emotional warm beat — it shows up sparingly and *means something* (you earned this). Red is desaturated to a soft coral so it warns without panicking.

**Imagery vibe:** cool, dark, slightly desaturated. Imagery (when used) leans toward photography of ambient lights, dusk skies, abstract glow — never warm/sunny stock photography. No grain, no b&w. Most "imagery" in this app is generative gradient + light, not photographic.

### Typography

- **Inter** for everything (V1). Variable weights 400 / 500 / 600 / 700 / 800.
- **Display sizes go BIG** — the timer reads at ~96px. Headers in dialogs are 28–32px. The brief mandates "large, elegant typography" centered in the orb.
- **Body copy** is 14–16px.
- **Numerals are tabular** (Inter has `font-feature-settings: 'tnum'` baked in for the timer) so the digits don't jiggle as the timer ticks.
- **Letter spacing**: tight on display (`-0.02em`), normal on body, slight positive (`0.02em`) on small all-caps labels (rare).
- **Line height** is generous on body (`1.5`), tight on display (`1.05`).

### Spacing

8px base grid. Tokens: `4 / 8 / 12 / 16 / 20 / 24 / 32 / 48 / 64`. Card inner padding is `20`. Screen edge padding is `20` on phone, `32` on tablet.

### Backgrounds

- **Ambient gradient layer** at the root — a slow-moving conic/radial gradient mixing navy → indigo → faint purple. This is *the* signature.
- **Depth layers**: at most 2 surface depths above bg (`--surface`, `--surface-2`). Cards float on the gradient, not on solid bg.
- **Subtle particles** — sparse, slow-drifting points of light at < 30% opacity. Optional, off in Hardcore mode.
- No repeating patterns. No textures. No hand-drawn illustrations. The app is *generative ambient*, not illustrated.

### Animation & motion

This is a **motion-first** product. Defaults:

- **Easing**: `cubic-bezier(0.22, 1, 0.36, 1)` ("ease-out-quint") for entrances. `cubic-bezier(0.65, 0, 0.35, 1)` ("ease-in-out-cubic") for state changes. `cubic-bezier(0.34, 1.56, 0.64, 1)` ("back-out") only for reward/celebration moments.
- **Durations**: micro `120ms` (button press), short `220ms` (chip toggle, fade), medium `400ms` (sheet open, screen transition), long `800ms` (orb breath cycle is `4s` on its own track).
- **Orb breath**: scale `0.96 → 1.04` over `4s`, infinite, ease-in-out-sine. Glow opacity follows.
- **Distraction shockwave**: a single 600ms ripple expanding from the orb center; orb wobbles `±2°` and desaturates briefly.
- **Session collapse** (Hardcore fail): 1200ms fade to black with a contracting blur. Dramatic but elegant — never violent.
- **No bouncing UI elements.** No springy menus. The bounce easing is reserved for reward dialogs only.
- **Reduced-motion fallback**: orb stops breathing, becomes static; transitions become 100ms fades.

### Hover / press states

- Mobile-first, so primarily **press** states.
- **Press**: scale `0.97`, brightness `0.92`, 120ms. No color shift.
- **Hover** (tablet / large): brightness `1.06`, 160ms. No translate.
- **Active chip**: filled with `--primary`, white text. **Inactive chip**: transparent fill, `--text-2` text, 1px border `rgba(255,255,255,0.08)`.

### Borders

- **Card border**: `1px solid rgba(255,255,255,0.06)` — barely visible, just enough to define glass edges.
- **Divider**: `1px solid rgba(255,255,255,0.08)`.
- **Focus ring** (a11y): `2px solid var(--primary)` with `2px` offset.
- Never use heavy black borders. Never use 2px+ borders except for focus rings.

### Shadows & elevation

Two systems coexist:

1. **Outer shadows** for cards / sheets / dialogs — soft, low-spread, dark.
2. **Inner glows** for the orb and active states — colored, blurred, additive.

Tokens:
- `--shadow-card` — `0 4px 16px rgba(0,0,0,0.32)`
- `--shadow-sheet` — `0 -8px 32px rgba(0,0,0,0.4)`
- `--shadow-dialog` — `0 24px 64px rgba(0,0,0,0.5)`
- `--glow-orb` — `0 0 80px 8px rgba(108,99,255,0.5)` (Focus); swaps per mode
- `--glow-success` — `0 0 32px 0 rgba(76,175,80,0.35)`
- `--glow-warning` — `0 0 32px 0 rgba(255,107,107,0.35)`

### Transparency & blur (glassmorphism)

Used **deliberately**, not everywhere.

- **Sheets and dialogs** use `backdrop-filter: blur(24px)` over `rgba(26, 26, 46, 0.72)`.
- **Distraction banner** uses blur(16px) over `rgba(255, 107, 107, 0.18)` with a 1px highlight border.
- **Cards do NOT blur** by default — they're solid `--surface`. Blur is for floating UI on top of the field.
- Avoid stacking blurs. One blur layer per scene.

### Corner radii

- `--r-xs` 4px — checkboxes, tiny tags.
- `--r-sm` 8px — input fields.
- `--r-md` 12px — small buttons.
- `--r-lg` 16px — cards (per spec).
- `--r-xl` 24px — sheets, large surfaces.
- `--r-pill` 999px — chips and pills (per spec).
- `--r-circle` 50% — orb, badges, score gauge.

### Cards

- Background `--surface` (`#1A1A2E`).
- Border `1px solid rgba(255,255,255,0.06)`.
- Radius `--r-lg` (16px).
- Padding `20px`.
- Shadow `--shadow-card`.
- No gradients on cards by default. (The gradient lives in the global background layer.)
- Selected / hovered card: bg becomes `--surface-2`.

### Layout rules

- **Bottom nav is fixed.** 64dp tall, `--surface` with 1px top divider. Active tab gets `--primary` icon + label.
- **Screen scroll regions** start below status bar, end above bottom nav.
- **Orb is centered** vertically in the upper 60% of the timer screen — controls live in the lower 40%.
- **Safe area** respect (Android: status bar 24dp, gesture nav 24dp).

---

## Iconography

> ⚠️ **No icon assets were provided in the source brief.** The Material 3 spec implies Material Symbols. I'm using **Material Symbols Rounded** as the canonical icon set for V1 — it matches the rounded card aesthetic (16dp card radius, pill chips) and is the natural fit for a Compose / Material 3 app. **Flag for the user**: confirm Material Symbols Rounded vs Outlined vs Sharp.

### Icon system

- **Family**: Material Symbols Rounded
- **Style axis**: Rounded (matches card radii)
- **Weight**: 400 default, 500 for active states
- **Fill axis**: 0 (outlined) by default, 1 (filled) for active bottom-nav tab and selected chips
- **Optical size**: 24dp default; 20dp inside chips; 28dp for bottom nav
- **Color**: inherits `currentColor`. Active = `--primary`, inactive = `--text-2`

### Loading

In the Android app, icons are loaded via Compose's `Icons.Rounded.*` from `androidx.compose.material.icons.material-icons-extended`. In the HTML UI kit, we load Material Symbols from the Google Fonts CDN — see `ui_kits/android/index.html`.

### Icon usage map

| Where | Icon | Notes |
|---|---|---|
| Bottom nav — Timer | `home` | Filled when active |
| Bottom nav — Analytics | `bar_chart` | Filled when active |
| Bottom nav — History | `list` | Filled when active |
| Bottom nav — Profile | `person` | Filled when active |
| Mode chips | `bolt` (Pomodoro), `psychology` (Deep Work), `school` (Study), `tune` (Custom) | Optional — chips can ship label-only |
| Strictness | `spa` (Relaxed), `shield` (Strict), `local_fire_department` (Hardcore) | |
| Distraction count | `notifications_active` | With red dot badge |
| Skip break | `skip_next` | |
| Pause / Stop / Start | `pause` / `stop` / `play_arrow` | Inside circular buttons |
| History row mode icons | reuse mode chip icons | |
| Best Focus Time card | `schedule` | |
| Onboarding step 2 | `visibility` (eye) | |
| Onboarding step 3 | `notifications` (bell) | |

### Logo / brand mark

> ⚠️ **No logo provided.** I generated a simple wordmark + concentric-ring symbol consistent with the orb metaphor — see `assets/logo.svg` and `assets/wordmark.svg`. **Please attach a real logo if one exists**, or we can iterate on this mark.

### Emoji-as-icon

Emoji is content, not iconography. The 🔥 streak counter, 🎉 completion dialog, 🏆 clean-session badge, and 😅 distraction banner are intentional — they carry emotional tone the icon system can't. They are *never* placed in nav, buttons, or chips.

### No unicode-character icons.

