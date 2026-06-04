# STITCH-READY PRODUCTION DESIGN HANDOFF SPECIFICATION
**Project Name:** DevSprint Tracker & Wake Alarm (Hackathon Tracker App)  
**Target Platform:** Android (Jetpack Compose / Material Design 3)  
**Author:** Principal Product Architect & Lead Design Systems Engineer  
**Handoff Target:** Stitch UI/UX Generative Agent / Frontend Engineering Team  

---

## 1. EXECUTIVE SUMMARY & PRODUCT VISION

### 1.1 Product Overview
The **DevSprint Tracker & Wake Alarm** is an offline-first, high-performance utility application designed for software developers, designers, and hackathon competitors. It blends event tracking (hackathons/sprints) with a highly customized, deterministic, and physically persistent local waking alarm system. 

*   **Primary Value Proposition:** Bridging event schedules and high-intensity hackathon deadlines with an un-ignorable waking system, ensuring that developers never sleep through critical sprint checkpoints, team presentations, or registration deadlines.
*   **Industry/Domain:** Developer Productivity, Event Tracking, Utilities, Time Management.
*   **Primary Business / User Goals:** Provide a single, dependable application to aggregate personal sprint agendas, parse upcoming global software hackathons, and tie individual events directly to customizable, high-volume device alarms (sound, vibration, notifications, with discrete snooze policies and visual profile reflection synced with servers).

---

## 2. SYSTEM ARCHITECTURE & CODEBASE ANALYSIS

Before redesigning the UI layer, Stitch must understand the existing constraints, modules, and workflows embedded in this project. All technical plumbing is already fully implemented—**do not modify any backend or database entities.**

### 2.1 Existing Controllers & Engine Boundaries
*   **Persistent Registry Database:** Powered by Room. The local database stores hackathon schedules, custom sprint records, registration statuses, alarm properties (duration, repetition cycle, delivery mode), and user settings.
*   **Alarm Dispatch Receiver (`ReminderAlarmReceiver`):** Utilizes `AlarmManager` for precise time triggers. Spawns `AlarmActivity` or shows notifications based on delivery type parameters.
*   **Cloud Synchronization Service (`FirebaseSyncService`):** Manages secure remote backup of user settings and profiles. User profiles (including the base64-encoded profile picture) sync up automatically in real-time. If a user logs in freshly or elsewhere, their profile photo will download and display seamlessly.
*   **State Containers (`HackathonViewModel`):** Exposes application state through reactive `MutableStateFlow` bindings. Tracks theme toggles, registration catalogs, live alarms, and active user profiles.

### 2.2 Technical Constraints & Boundaries (Non-Negotiable)
1.  **Do Not Create New Screens or Workflows:** Keep the screen inventory limited to the four defined tabs: **Dashboard**, **Profile**, **Settings**, and **About**.
2.  **Strict Profile Image Processing:** Uploaded profile photos are parsed as Base64 strings. When a photo is picked, we invoke `CropImageDialog` which resizes, scales, and crops the selected bounding area to a compressed, 250x250 format decoded with `Base64.NO_WRAP`. This ensures the photo is saved properly, small enough to sync on Firestore, but highly sharp on screens.
3.  **No Unselected Audio Titles:** The sound track name storage for custom ringtones is eliminated; all full-alarm playback is standardized using the high-clarity `Default` device alarm channel to avoid volume loss or device sound-muting errors.
4.  **No Stars in "About":** The visual implementation of the "About" screen must not contain decorative, uncoordinated vector stars; use clean typography hierarchy and elegant product spacing.

---

## 3. USER PERSONAS, GOALS & PAIN POINTS

### 3.1 Primary Persona: "The Midnight Hackathoner" (Nitin)
*   **Profile:** 20-year-old computer science student and competitive builder. Often codes in blocks from 11 PM to 5 AM.
*   **Core Motivations:** Joining high-stakes global developer hackathons, staying on top of project visual designs, and getting exactly 3 hours of highly precise sleep without missing final team commits or pitching deadlines.
*   **Key Pain Point:** Alarm applications with tiny touch targets, easy-to-dismiss snooze sliders, or muddy color schemes that blend too much into the background context under night-light conditions.

---

## 4. SCREEN INVENTORY & DETAIL AUDIT

### 4.1 "DASHBOARD" SCREEN (Main View)
*   **Primary Purpose:** Main hub displaying active developer sprint cards, registered track list, and search catalogs of global events.
*   **User Action Inventory:**
    *   Initialize custom sprint creation using a Floating Action Button (FAB).
    *   Toggle between "Find Sprints" web-catalogs and user-joined "My Sprints" tabs.
    *   Configure alarm parameters: select reminder days, type custom schedules, and adjust alarm duration using a discrete 5-unit slide bar.
    *   De-register or modify alarm properties on individual cards.
*   **Current Visual Layout Quirks:** 
    *   The "registered sprint cards" need an unmistakable layout difference from "search result cards" so that what is currently active is immediately recognizable.
    *   The spacing between the category headers and cards needs more generous default padding (use a grid format of 16dp).

### 4.2 "PROFILE" SCREEN
*   **Primary Purpose:** High-fidelity overview of the user identity, joined challenges, and interactive profile photo configuration.
*   **Key Functional Items:**
    *   PFP circular avatar box displaying the processed Base64 image using a dynamic bitmap painter.
    *   Clicking the avatar launches the customized image picker and immediate `CropImageDialog` overlay.
    *   Display of cumulative statistics: Sprints registered, active trackers, and completion scores.
*   **Current Visual Layout Quirks:**
    *   The PFP circle container must be framed clearly. In light theme, it must have a prominent primary border so it doesn't wash out against white surface cards.

### 4.3 "SETTINGS" SCREEN
*   **Primary Purpose:** Global parameters management including default alarm durations, delivery channel types (Full Alarm, Vibration, Notification), and the visual theme override.
*   **Key Functional Items:**
    *   Theme Mode Select Pill-Button: High-contrast toggling control.
    *   Snooze timer adjustment from 1 minute to 15 minutes.
    *   Discrete 5-unit playback slider ranging from 10 seconds to 120 seconds.
*   **Current Visual Layout Quirks:**
    *   The theme switch ball has been updated into a beautiful, high-contrast Slate/Pill Switch. Stitch must avoid using standard unbordered switches which blend into background surfaces. Ensure the toggle is visually striking in both themes.

### 4.4 "ABOUT" SCREEN
*   **Primary Purpose:** Project version identification and credits.
*   **Key Functional Items:** Simple text blocks detailing copyright, credits, and version history.
*   **Current Visual Layout Quirks:**
    *   Remove all decorative vector stars. Lean on premium display typefaces and generous margins to feel professional and clean.

---

## 5. DESIGN SYSTEM FUNDAMENTALS

Stitch MUST adhere strictly to the following parameters for standard visual controls:

### 5.1 Spacing & Density System (Material 3 Adaptive)
Measurements are calculated on an 8dp structural grid. Avoid raw floating point measurements.

| Space Token | DP Value | Primary Application |
| :--- | :--- | :--- |
| `spacing_xxs` | 4.dp | Tight vertical stacks of supplementary labels and sub-rows. |
| `spacing_xs` | 8.dp | Padding within small buttons, labels, and micro-icons. |
| `spacing_sm` | 12.dp | Distance between sibling controls inside high-density cards. |
| `spacing_md` | 16.dp | General baseline grid padding around screens and large component margins. |
| `spacing_lg` | 24.dp | Stack separations between unrelated vertical sections (dashboard cards). |
| `spacing_xl` | 32.dp | Outer visual boundaries on expanded-screen tablets. |

### 5.2 Responsive Border Radius
Make active UI buttons and containers rounded with varying weights for modern structural hierarchy.
*   **Small (`RoundedCornerShape(6.dp)`):** Checkboxes, small micro-tags, filter chips, and badges.
*   **Medium (`RoundedCornerShape(12.dp)`):** Dialog backgrounds, card inner components, and slider containers.
*   **Large (`RoundedCornerShape(16.dp)`):** Sprint list cards, profile container blocks, and bottom action drawers.
*   **Pill (`RoundedCornerShape(50)` / `CircleShape`):** Profile picture holders, theme switch pill selectors, main trigger FABs.

### 5.3 Elevation & Surface Layering
M3 relies on tonal color tinting instead of complex drop shadows. Stitch should use elevation sparingly to keep lookups performant:
*   **Level 0 (Flat):** Main background, dark and light canvases.
*   **Level 1 (Low surface):** Regular search result cards, secondary chips.
*   **Level 2 (Hover/Active):** Registered active sprint cards.
*   **Level 3 (Modal):** Input dialogs, `CropImageDialog` overlay.

---

## 6. MULTI-THEME DESIGN SPECIFICATIONS

Stitch must render the user interface across several distinct themes, ensuring the theme toggle behaves as an eye-safe, high-contrast component. In all themes, structural elements must use standard color scheme identifiers (`MaterialTheme.colorScheme.surface`, etc.) to maintain code consistency.

### 6.1 THEME A — Modern SaaS Professional (Light Mode)
*   **Aesthetic Philosophy:** Ultra-high clarity, bright white spaces paired with subtle cool gray grids. Deep royal blue accents for actionable focal points.
*   **Key Palette Specs:**
    *   Background: `#F8FAFC` (Slate 50)
    *   Surface Container: `#FFFFFF` (Solid White)
    *   Primary Accent: `#2563EB` (Royal Blue)
    *   On-Primary Text: `#FFFFFF`
    *   Neutral Text High-Contrast: `#0F172A` (Slate 900)
    *   Neutral Text Low-Contrast: `#64748B` (Slate 500)
    *   Card Border Color: `#E2E8F0` (Slate 200) with 1.5.dp thickness.
*   **Visual Strategy:** Card outlines are crisp, and interactive elements feature noticeable ripples. Text uses tight geometric sans-serif fonts to look neat and clear.

### 6.2 THEME B — Premium Developer Obsidian (Dark Mode)
*   **Aesthetic Philosophy:** Premium midnight canvas built using dark charcoal sheets. Accents are glowing turquoise cyan, creating a modern developer environment that is comfortable for late-night work.
*   **Key Palette Specs:**
    *   Background: `#090D16` (Deep space base)
    *   Surface Container: `#161F30` (Obsidian Blue Card)
    *   Primary Accent: `#06B6D4` (Teal Cyan Glow)
    *   On-Primary: `#000000` (Max contrast on glowing teal)
    *   On-Surface Headline: `#F8FAFC` (Crisp text off-white)
    *   Active State Frame: `#0E7490` (Deep teal border outline for active indicators)
*   **Visual Strategy:** Low-contrast elements use semitransparent obsidian layers. Highly active alarm statuses show up with strong color fills.

### 6.3 THEME C — Playful Pastel (Cute)
*   **Aesthetic Philosophy:** Whimsical, cozy workspace using soft cream canvases, rounded bubbles, and friendly pastel lilac/strawberry accents.
*   **Key Palette Specs:**
    *   Background: `#FFFDF9` (Warm Milk Cream)
    *   Surface Container: `#FFF5F5` (Soft Pink Cream Card)
    *   Primary Accent: `#E8A2A5` (Soft strawberry rose)
    *   Secondary Pastel: `#C5D3E8` (Lilac Blue)
    *   Border Treatment: `#F5D6D6` matching soft line frames.
*   **Visual Strategy:** Soft, friendly roundness. Checkboxes and cards use `RoundedCornerShape(20.dp)` with thick borders and simple line strokes.

### 6.4 THEME D — Cyberpunk Neon Grid (Vaporwave)
*   **Aesthetic Philosophy:** High-octane sci-fi gaming deck. Uses deep purple matrices highlighted by hot pink borders, neon green parameters, and sharp angles.
*   **Key Palette Specs:**
    *   Background: `#05020C` (Synthesizer Dark Purple Void)
    *   Surface Card: `#120924` (Cyber purple capsule)
    *   Primary Accent: `#F43F5E` (Hot Magenta Pink)
    *   Secondary Accent: `#10B981` (Acid Green for alarm active timers)
*   **Visual Strategy:** Bright, high-contrast design. Content containers use sharp borders, thin colored lines, and uppercase neon labels.

### 6.5 THEME E — Classy Luxury (Premium Executive)
*   **Aesthetic Philosophy:** Sophisticated and refined interface, drawing inspiration from high-end devices and mechanical watches. Uses warm charcoal backgrounds with champagne gold accents.
*   **Key Palette Specs:**
    *   Background: `#111111` (Matte Black Onyx)
    *   Surface Container: `#1A1A1A` (Brushed alloy sheet)
    *   Primary Accent: `#D4AF37` (Champagne Gold highlight)
    *   On-Secondary Label: `#A3A3A3` (Muted silver)
*   **Visual Strategy:** Minimalist, thin, precise outlines, and elegant typography paired with clean, generous layouts.

### 6.6 THEME F — Cozy Minimalist (Focus Mode)
*   **Aesthetic Philosophy:** Calming, distraction-free layout to foster flow and productivity. Uses natural clay colors with quiet typography.
*   **Key Palette Specs:**
    *   Background: `#F2F0EB` (Clay Pebble Warm Gray)
    *   Surface Card: `#E6E4DD` (Mineral gray card sheet)
    *   Primary Accent: `#4A554A` (Sage Green Forest)
    *   Inert Labels: `#8C8A82`
*   **Visual Strategy:** Flat cards, elegant dark typography, thin lines, and comfortable negative space.

### 6.7 RECOMMENDED PRODUCT DIRECTION — "THE ULTIMATE AMBIENT SPACE THEME"
This is the **primary recommendation** for Stitch to use when assembling the mockups.
*   **Why it's best:** It combines the comfort of late-night coding screens with clear, high-contrast action buttons. Interactive elements stand out clearly, and the layout uses a clean space theme that speaks directly to developers.
*   **Target Audience:** Sleep-deprived builders, hackathon competitors, and product managers who require clear visual hierarchies and easy interaction flows during tight project marathons.

---

## 7. EXHAUSTIVE ICONOGRAPHY SYSTEM

We strongly recommend **Lucide Icons** as the primary family due to its uniform stroke weight (2.dp) and elegant, clean appearance. Alternatively, Stitch can use the latest filled and outlined variants of **Material Symbols**.

### 7.1 Tab Bar Icons (Standard Outlines)
*   `Dashboard` -> **Primary Icon:** `lucide/Terminal` (Coding sprint center) | **Alternative:** `lucide/Clock`
*   `Profile` -> **Primary Icon:** `lucide/UserCircle` | **Alternative:** `lucide/Contact`
*   `Settings` -> **Primary Icon:** `lucide/Settings2` | **Alternative:** `lucide/Sliders`
*   `About` -> **Primary Icon:** `lucide/HelpCircle` | **Alternative:** `lucide/Info`

### 7.2 Core Actions Map
*   `Create custom sprint` -> **Primary:** `lucide/Plus` (Clean, centered icon) | **Style:** Outlined stroke.
*   `Save Default Settings` -> **Primary:** `lucide/CheckSquare` (Confirms and saves parameters clearly) | **Style:** Solid thick fill.
*   `De-register sprint / delete` -> **Primary:** `lucide/ClockAlert` (Warns of alarm removal) | **Style:** Red outline.
*   `Crop action confirm` -> **Primary:** `lucide/Crop` | **Style:** High-contrast blue toggle button.

---

## 8. INTERACTIVE COMPONENT DETAILS

### 8.1 NEW HIGH-CONTRAST THEME SELECTION TOGGLE SWITCH
Replaces complex individual click buttons with a sleek, standard Material 3 Toggle Switch.
*   **State Matrix:**
    *   **Light Theme Active:** Switch thumb is positioned to the left. Background track is a clear, low-contrast neutral slate.
    *   **Dark Theme Active:** Switch thumb slides smoothly to the right, colored with high-contrast primary accent colors to draw focal interest.

### 8.2 BASIC CONTINUOUS ALARM PLAYBACK BAR
Designed as a beautifully clean, simple, and standard slider control without any step dots or customized tick lines.
*   **Aesthetic Detail:** The playback duration bar renders as a clean continuous progress bar. The dragging thumb has a minimal, rounded structure that slides smoothly between 10 seconds and 120 seconds.
*   **State Design:**
    *   No tick steps or visual subdivision dots along the track.
    *   Value snaps smoothly and is displayed in real-time above the slider (e.g. "Alarm Playback: 30 Seconds").

### 8.3 THE PROFILE PICTURE POPUP & GESTURAL CROP OVERLAY
When users tap the circular profile picture area, they can view the full photo in a centered detailed popup with options to dismiss or edit. Tapping the "Edit Photo" action directly opens the file picker to choose a photo and open the custom gestural scaling and positioning dialog.

```
+------------------------------------------+
|          Crop and Size Photo             |
|                                          |
|                /---------\               |
|               /           \              |
|              |    [PFP]    |             |  <-- 160.dp Circular viewport preview
|               \           /              |      Responds to multi-touch gestures
|                \---------/               |
|                                          |
|        Pinch to Zoom • Drag to Pan       |  <-- Direct touchscreen gesture guide
|                                          |
|     [ Cancel ]         [ Crop & Apply ]  |  <-- Flat layout button actions
+------------------------------------------+
```
*   **UX Best Practice:** The dialog completely removes clunky sliders and handles resizing and placement through natural touchscreen touch gestures (pinch-to-zoom and drag-to-pan) on the image itself, matching standard premium mobile applications.

---

## 9. SCREEN-BY-SCREEN SPECIFICATION

Stitch must use the detailed layouts below to build mockups for each screen:

### 9.1 Dashboard View - Active & Registered Modes

#### Current Layout Review
Currently, the search catalog items ("Find Sprints") and registered items ("My Sprints") use cards with very similar layouts. This makes it difficult to quickly distinguish between joined and unjoined events on a small screen.

#### Layout Organization & Information Hierarchy
*   **Level 1 (Top Hero Dashboard):** A prominent count block that sums up the current week's active events.
*   **Level 2 (The Active Alarm Block):** Displays the next upcoming alarm trigger at a glance.
*   **Level 3 (Tab Containers):** Easy-to-use search and active list toggles, spaced with clean 16dp margins.
*   **Level 4 (Card Differences):**
    *   *Search Card:* Styled with lightweight borders and a clean primary action button like "Register Alarm."
    *   *Joined Active Card:* Styled with a soft background fill. The alarm properties are displayed clearly (e.g., `🔔 Alarm • 30s • Once`). It features an option to update snooze parameters or un-register without clutter.

---

## 10. RESPONSIVE DESIGN RULES & ADAPTATION MATRIX

### 10.1 Mobile Layouts (Compact Width < 600.dp)
*   **Tabs Representation:** Implemented as a clean, high-contrast bottom navigation bar with clear icon markers.
*   **Card Densities:** Cards span the full width of the screen, with 16dp outer margins for modern, easy-to-read layouts.

### 10.2 Table / Drawer Layouts (600.dp to 840.dp)
*   **Tabs Representation:** Screen transitions seamlessly into a side navigation rail to preserve vertical screen space.
*   **Grid layout:** Search cards are arranged in a neat 2-column grid layout with consistent, automatic vertical heights.

### 10.3 Desktop / Large Screens (Width > 840.dp)
*   **Grid Layout:** Grid columns expand to a 3-column layout. Main layout blocks are capped at `max = 960.dp` width, centered on the screen, to prevent lines from stretching awkwardly.

---

## 11. ACCESSIBILITY COMPLIANCE AUDIT

Every component designed by Stitch must pass the checks below before being approved for development:

1.  **Touch Target Areas:** Buttons, tabs, and filter chips must have clear touch areas of at least **48.dp x 48.dp** (`minimumInteractiveComponentSize`).
2.  **Color Contrast Requirements:** Label text elements must maintain a contrast ratio of at least **4.5:1** against their background surfaces (compliant with WCAG AA guidelines). Highly active elements should strive for **7:1** contrast.
3.  **Keyboard & Screen Reader Aids (`ContentDescriptions`):** All vector icons and custom buttons require detailed descriptions (e.g., `contentDescription = "User profile picture border"`). This ensures screen readers like TalkBack can easily parse and navigate the interface.

---

*This document contains all the necessary layout instructions, design parameters, and theme rules to guide your design process. Use it to build beautiful, high-clarity assets for our developer audience!*
