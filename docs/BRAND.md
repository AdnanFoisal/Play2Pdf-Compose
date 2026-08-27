# Play2PDF Brand Reference (canonical)

**Status: authoritative.** Where any other document disagrees on color, this
file wins. In particular, the violet palette (`#7C5CFF`, `#a78bfa`,
`#8b5cf6`) in `mock assests/*.html`, `docs/old plans/*`, and
`docs/newplans/*` is **superseded** — those were pre-rebrand references.

## Decision

The 2026-07 rebrand (`git 0563842` — "Spotify-inspired app icon") moved the
product to a **green identity**. The code, launcher icon, and splash were
updated; the docs and XML resources were not. This file locks the green as
canonical and realigns everything else to it.

## Core palette

| Token | Hex | Use |
|---|---|---|
| Brand | `#1DB954` | Primary accent, active states, links |
| BrandStrong | `#1ED760` | Hover/bright accent, gradient end |
| BrandDeep | `#12873B` | Gradient start, pressed states |
| BrandMid | `#18A048` | Gradient middle |
| BrandGradEnd | `#00D1FF` | Cyan gradient end (CTA, ring, sparkline) — playful contrast, never a background |

**Primary gradient** (CTA button, compile ring, sparkline stroke):
`#12873B → #18A048 → #00D1FF`

## Surfaces (dark, green-tinted)

| Token | Hex |
|---|---|
| Bg (app base) | `#060907` |
| Surface0 | `#080C0A` |
| Surface1 (cards) | `#121B15` |
| Surface2 (rows) | `#18241C` |
| Surface3 (history) | `#0E1611` |
| Border hairline | white @ 6% |
| Border strong | white @ 12% |

## Atmosphere glows

| Token | Hex | Placement |
|---|---|---|
| GlowGreen | `#0D2415` | Top-center radial (Home, Settings, Onboarding) |
| GlowTeal | `#0A1F1E` | Bottom-right radial (History) |
| GlowDeep | `#07120A` | Compiling vertical-fade mid stop |

## Text

Primary `#FFFFFF` · Secondary `#A1A1AA` · Tertiary `#71717A` · Quaternary `#52525B`

## Typography

- Display/headings: **Space Grotesk** (bundled `res/font/space_grotesk.ttf`)
- Body: **DM Sans** (bundled `res/font/dm_sans.ttf`)

Both are bundled variable TTFs — no downloadable fonts, no Play Services
dependency. (The interim "Inter via GoogleFont provider" approach was
removed in A2; Inter remains the PDF/UI reference only where already
shipped in legacy screenshots.)

## Also superseded by this file

- `values/colors.xml` violet brand tokens → green (aligned in A1)
- Splash icon gradient (`#FF512F → #DD2476`) → brand gradient (aligned in A1)
- `Color.kt` comments describing the violet palette (rewritten in A1)
- Naming: `GlowViolet`/`GlowIndigo` → `GlowGreen`/`GlowTeal` (they already held green/teal values)
