---
name: CareFlow Myanmar
colors:
  surface: '#fdf9ee'
  surface-dim: '#dddacf'
  surface-bright: '#fdf9ee'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f7f4e8'
  surface-container: '#f1eee3'
  surface-container-high: '#ebe8dd'
  surface-container-highest: '#e5e2d7'
  on-surface: '#1c1c15'
  on-surface-variant: '#3f4947'
  inverse-surface: '#313129'
  inverse-on-surface: '#f4f1e5'
  outline: '#6f7977'
  outline-variant: '#bec9c6'
  surface-tint: '#216962'
  primary: '#00433e'
  on-primary: '#ffffff'
  primary-container: '#0d5c56'
  on-primary-container: '#8ed2ca'
  inverse-primary: '#8fd3cb'
  secondary: '#904d00'
  on-secondary: '#ffffff'
  secondary-container: '#fe932c'
  on-secondary-container: '#663500'
  tertiary: '#00442d'
  on-tertiary: '#ffffff'
  tertiary-container: '#005e40'
  on-tertiary-container: '#66daa8'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#abefe7'
  primary-fixed-dim: '#8fd3cb'
  on-primary-fixed: '#00201d'
  on-primary-fixed-variant: '#00504a'
  secondary-fixed: '#ffdcc3'
  secondary-fixed-dim: '#ffb77d'
  on-secondary-fixed: '#2f1500'
  on-secondary-fixed-variant: '#6e3900'
  tertiary-fixed: '#85f8c4'
  tertiary-fixed-dim: '#68dba9'
  on-tertiary-fixed: '#002114'
  on-tertiary-fixed-variant: '#005137'
  background: '#fdf9ee'
  on-background: '#1c1c15'
  surface-variant: '#e5e2d7'
typography:
  display-hero:
    fontFamily: plusJakartaSans
    fontSize: 56px
    fontWeight: '700'
    lineHeight: 64px
    letterSpacing: -0.02em
  display-hero-mobile:
    fontFamily: plusJakartaSans
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.015em
  headline-lg:
    fontFamily: plusJakartaSans
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.015em
  headline-lg-mobile:
    fontFamily: plusJakartaSans
    fontSize: 26px
    fontWeight: '600'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: plusJakartaSans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: plusJakartaSans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.005em
  title-token:
    fontFamily: plusJakartaSans
    fontSize: 40px
    fontWeight: '800'
    lineHeight: 44px
    letterSpacing: 0.04em
  body-lg:
    fontFamily: inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: inter
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.005em
  label-sm:
    fontFamily: inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  caption-caps:
    fontFamily: inter
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.06em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  space-3xs: 0.125rem
  space-2xs: 0.25rem
  space-xs: 0.5rem
  space-sm: 0.75rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
  space-2xl: 2.5rem
  space-3xl: 3.5rem
  space-4xl: 5rem
  gutter-mobile: 1rem
  gutter-desktop: 1.5rem
  margin-mobile: 1.25rem
  margin-tablet: 2rem
  margin-desktop: 3rem
---

## Brand & Style

This design system reimagines the clinical environment by replacing sterile, intimidating corporate blues with the grounded, calming warmth of therapeutic hospitality. It serves patients, front-desk triage staff, clinic coordinators, and physicians navigating complex appointment flows in modern private hospitals and outpatient facilities.

The core visual philosophy combines **Warm Tactile Modernism** with high-clarity clinical utility:
- **Atmosphere:** Organic, hospitable, and serene. It communicates the empathy of high-end wellness spaces while maintaining the uncompromising precision required for healthcare logistics.
- **Emotional Resonance:** Alleviates waiting anxiety, restores dignity and autonomy to patients, and grants medical staff high-efficiency visual control without cognitive exhaustion.
- **Visual Expression:** Layered surfaces of warm alabaster and unbleached cream, structured by delicate warm-oat hairline dividers, illuminated with deep Aegean teal authority, and accented with restorative amber and terracotta cues. Tactile card surfaces, subtle ticket-inspired visual motifs for token numbering, and generous negative space eliminate the chaotic visual noise typical of clinical queue screens.

## Colors

The palette grounds clinical interactions in an organic, calming spectrum. Light mode is the exclusive operating environment to preserve clinical legibility, thermal comfort in brightly lit spaces, and the visual tactility of paper-and-linen healthcare design.

### Core Roles
- **Primary (Aegean Teal - `#0D5C56`):** Primary interactions, authoritative actions, active navigation anchors, and dominant branding elements. Interactive states employ `#0A4A45` for hover and pressed depths.
- **Secondary (Warm Amber/Terracotta - `#D97706`):** High-priority queue callouts, in-progress state monitors, and vital patient progress markers. Accent states step up to Terracotta (`#EA580C`) or Honey Gold (`#F59E0B`) for visual interest.
- **Tertiary & Status Accents:**
  - **Success / Completed / Available:** Mint Sage (`#059669` foreground, `#D1FAE5` surface).
  - **Pending / Waiting / In-Queue:** Golden Honey (`#D97706` foreground, `#FEF3C7` surface).
  - **Critical / Emergency / Urgent:** Soft Coral Rose (`#E11D48` foreground, `#FFE4E6` surface).

### Surfaces & Backgrounds
- **App Canvas:** Warm Alabaster (`#FBFBF8`) providing warm daylight illumination without clinical glare.
- **Surface Level 1 (Card & Module Canvas):** Pure Warm White (`#FFFFFF`).
- **Surface Level 2 (Nested Panels / Data Shelves):** Warm Cream (`#F5F4EE`).
- **Structural Dividers & Outlines:** Warm Oat Sand (`#E6E3D8` primary border, `#D8D4C5` reinforced border).

### Typography & Content
- **Deep Pine Slate (`#162A27`):** Primary headings, patient queue token numerals, and critical telemetry data.
- **Sub-Slate Charcoal (`#374744`):** Primary readable body copy, form labels, and table values.
- **Muted Sage Grey (`#6B7B78`):** Secondary metadata, timestamps, helper descriptions, and inactive states.

## Typography

The typographical structure balances emotional accessibility with high-efficiency clinical utility through a two-tiered pairing:

- **Headlines & Token Numbers (Plus Jakarta Sans):** Its geometric precision combined with subtle rounded terminals delivers warmth and approachable modernity without appearing informal or unscientific. The `title-token` variant is engineered specifically for physical token IDs (e.g., `A-108`, `P-042`) on TV displays and queue cards, optimized for high optical legibility across long viewing distances.
- **Clinical Data, Tables & UI Body (Inter):** Applied across patient charts, diagnostic metrics, timestamps, and dense administrative forms. Its neutral architecture and tabular numerals ensure zero optical drift across dynamic time counters, wait lists, and vital stats.
- **Case Rhythms:** Overline labels and table header titles must use `caption-caps` with uppercase transformation and explicit tracking (`0.06em`) for immediate perceptual grouping.

## Layout & Spacing

This design system uses a strict 8-point harmonic spacing grid (with a 4-point micro-scale for compact form items and status tags).

### Grid Architecture
- **Desktop (1200px+):** 12-column responsive fluid grid with max-width containment at `1440px`. Gutters are fixed at `24px` (`1.5rem`), outer margins at `48px` (`3rem`).
- **Tablet (768px - 1199px):** 8-column layout with `20px` gutters and `32px` margins. Clinical overview modules reflow from a 4-up stack into a 2x2 matrix.
- **Mobile (Below 768px):** 4-column layout with `16px` gutters and `20px` margins. Horizontal side-scrolling pills are preferred for service-counter filters rather than cramped vertical chip stacks.

### Rhythm & Density
- **Triage and Clinical Consoles:** Compact vertical spacing (`space-xs` to `space-sm`) between data rows, with fixed minimum touch/click targets of 44px to prevent medical input error under high pressure.
- **Public Displays & Patient-Facing Views:** Relaxed spatial hierarchy utilizing `space-xl` through `space-3xl` section separators to project calm control.

## Elevation & Depth

Visual depth is achieved through **Tonal Layering** combined with soft, warm-tinted ambient shadows. Sterile grey dropshadows are strictly forbidden; all drop shadows are infused with a hint of warm pine-slate to mimic natural indoor amber light.

- **Level 0 (Flat Base):** Applied to the Warm Alabaster (`#FBFBF8`) canvas and static fieldsets. Borderless.
- **Level 1 (Card & Module Shells):** 
  - Background: `#FFFFFF`
  - Border: 1px solid `#E6E3D8`
  - Shadow: `0px 2px 6px rgba(22, 42, 39, 0.03), 0px 8px 18px rgba(22, 42, 39, 0.04)`
- **Level 2 (Active Queue Items, Hover States, Floating Controls):**
  - Background: `#FFFFFF`
  - Border: 1px solid `#D8D4C5`
  - Shadow: `0px 4px 12px rgba(22, 42, 39, 0.06), 0px 16px 32px rgba(22, 42, 39, 0.05)`
- **Level 3 (Modals, Triage Overlays, Flyouts):**
  - Background: `rgba(255, 255, 255, 0.94)` paired with `backdrop-filter: blur(12px)`
  - Border: 1px solid `#E6E3D8`
  - Shadow: `0px 12px 32px rgba(22, 42, 39, 0.08), 0px 24px 60px rgba(22, 42, 39, 0.06)`
- **Backdrop Scrim:** `rgba(22, 42, 39, 0.35)` with an 8px lens blur to focus focus on critical clinical dialogs.

## Shapes

The shape hierarchy establishes an organic, modern sanctuary feel. It avoids severe technical right angles while strictly avoiding playful or child-like pill forms on foundational containers.

- **Primary Cards & Panels:** Standard `16px` (`rounded-lg`) curvature. Provides structural containment with gentle humanistic softening.
- **Large Contextual Hero Cards / Modals:** `20px` to `24px` (`rounded-xl`) outer boundary radius.
- **Inputs, Buttons, and Select Menus:** Structured `10px` to `12px` radius, balancing crisp alignment with comfortable fingertip affordance.
- **Queue Tokens, Tags, and Pill Badges:** Full circular rounding (`9999px`) exclusively reserved for inline status indicators, patient category labels, and micro-counters to visually contrast against rectangular structured patient panels.

## Components

### 1. Buttons & Triggers
- **Primary:** Background `#0D5C56`, text `#FFFFFF`, border none, 12px radius, font weight 600. Hover transitions to `#0A4A45` with subtle 2px vertical lift.
- **Secondary (Tertiary Actions):** Background `#F5F4EE`, text `#162A27`, 1px solid `#E6E3D8`. Hover shifts to `#E6E3D8`.
- **Urgent / Emergency Trigger:** Background `#E11D48`, text `#FFFFFF`, subtle coral outer glow for instantaneous emergency triage actions.

### 2. Queue Token Badges
- **Visual Design:** Ticket-inspired structural component. White or ivory surface with a subtle dashed divider separating the counter station from the queue identifier.
- **Typography:** Token ID rendered in `title-token` (`#162A27`), station designation in `caption-caps` (`#6B7B78`).
- **Urgency Indicator:** Left-hand 4px color stroke matching the assigned triage category (Mint Sage for General, Warm Amber for Priority, Soft Coral for Critical).

### 3. Patient Status Chips
- Height: 26px, full pill shape (`rounded-full`), horizontal padding of 10px.
- **Waiting:** `#FEF3C7` background, `#D97706` text, with a pulsing 6px golden amber dot indicator.
- **In-Consultation:** `#D1FAE5` background, `#059669` text, solid 6px mint dot.
- **Emergency Delay:** `#FFE4E6` background, `#E11D48` text.

### 4. Input Fields & Form Controls
- Height: 44px (touch standard).
- Base: White background, 1px solid `#E6E3D8` border, 10px radius, text in Sub-Slate (`#374744`).
- Focus State: 1px solid `#0D5C56` paired with an ambient ring: `0 0 0 3px rgba(13, 92, 86, 0.15)`. No harsh primary blue rings.
- Checkboxes & Radios: Aegean teal fill with crisp white check/radio dot; unchecked border in `#D8D4C5`.

### 5. Tactical Stat Cards
- Layout: Top-level summary metric cards (Average Wait, Active Patients, Doctors on Duty).
- Style: Base `#FFFFFF`, micro-gradient sheen running from `#FFFFFF` down to `#FBFBF8`, bounded by 1px `#E6E3D8`. Displays numeric KPIs in Plus Jakarta Sans 32px/700, topped with 11px uppercase oat-slate subtitles.

### 6. Clinical Data Tables
- Header Row: Background `#F5F4EE`, uppercase 11px text in `#6B7B78`, height 44px, bottom border 1px solid `#E6E3D8`.
- Table Rows: Alternating hover state with `#FBFBF8`, bottom border 1px solid `#F5F4EE`. Cell padding: 14px 16px. All clinical numerical identifiers (MRN, Age, Wait Time) rendered in Inter Tabular Numerals.