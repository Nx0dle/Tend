# Tend UI language

This is the source of truth for new screens and refinements. Reuse the shared tokens in `TendTheme.kt`; do not introduce one-off spacing, corner, or animation values without a clear interaction reason.

## Visual direction

Tend should feel calm, precise, and considered: warm porcelain canvas, deep forest for primary emphasis, sage for quiet supporting surfaces, and restrained brass gold for highlights. Ink carries primary text; muted green-gray carries secondary text. White is reserved for raised content surfaces. Red is reserved for actual errors or overspending states. Keep the palette consistent and avoid adding decorative colors.

Colors that carry meaning have named tokens next to the base palette: `OnHeroMuted` for labels/metrics on deep forest surfaces, `Placeholder` for empty-input hints, `Reserve` and `Overspend` for the allocation bar, and `NoticeText` for the "80% limitu" badge. Use these tokens instead of writing raw hex in screens.

The app icon repeats the header mark: a modest, optically centered lowercase Roboto Bold “t” in sage (`#DCEBE2`) on forest green (`#176451`). Keep the launcher artwork to this single letter on the solid field; no coin ring, extra outline, or label. Use Android's adaptive icon and matching monochrome layers. Keep the glyph centered and surrounded by generous negative space.

## Layout rhythm

Use `TendSpace` as the spacing scale:

| Token | Size | Use |
| --- | ---: | --- |
| `xsmall` | 4 dp | Tight icon/label and internal alignment |
| `small` | 8 dp | Compact control gaps |
| `medium` | 12 dp | Related content groups |
| `large` | 16 dp | Default card padding and component gaps |
| `xlarge` | 24 dp | Screen gutters and major section breaks |
| `xxlarge` | 32 dp | Rare, deliberate separation |

Prefer a small number of clear groups over many nested cards. Keep screen gutters and repeated section spacing aligned. Controls must provide at least a 44 dp touch target even when their visible icon is smaller.

Use `TendSize.control` (44 dp) as the shared height for compact pill controls so paired selectors (such as the expense/income toggle and the category selector) stay visually matched. 44 dp is also the minimum touch target, so a control of this height needs no extra hit-slop.

## Shapes

Use `TendRadius.small` for small chips and marks, `control` for fields and compact controls, `card` for content cards, and `panel` for primary summary surfaces. Use `CircleShape` for circular marks, icon buttons, and floating pill navigation bars (where container and items share the pill curvature to remain strictly concentric). Avoid mixing arbitrary rounded values in the same component family.

## Motion

Use `TendMotion.quick` for selection feedback, `standard` for navigation and content transitions, and `expressive` only for a larger state change. Progress indicators use `progress`. Motion should explain a state change, preserve layout, and remain short; avoid animating decorative elements or delaying input feedback.

## Typography

Use the `TendType` roles in `TendTheme.kt` for repeated UI text instead of ad-hoc sizes: `heroLabel`/`heroAmount` for summary surfaces, `monthTitle` for the shared header, `sectionTitle` for section headers, `rowTitle`/`rowAmount` for list rows, `caption`/`micro` for supporting text, `badge` for status tags, `amount` for entry input, `controlLabel`/`compactLabel` for controls, and `navLabel` for navigation. Reserve explicit font sizes for one-off decorative glyphs.

## Calendar

The month grid communicates dates and spending density, not transaction amounts. Each date remains easy to tap; a small, restrained marker indicates that expenses exist, with intensity showing relative daily spend. The markers are self-explanatory: do not add a separate legend under the grid. The selected date is the only strongly filled cell. Show exact totals and ledger rows in the selected-day detail, where they have room to remain readable. The monthly total belongs in one clear summary surface.

## Component consistency

Use the shared palette, spacing, shape, and motion tokens in cards, forms, category selectors, dialogs, and navigation. Give every visible label a distinct job: avoid repeating the selected month, section title, or value in adjacent captions. Use `TendBadge` for concise status tags (`Tylko podgląd`, `Z wyprzedzeniem`, `Ponad limit`, `80% limitu`, `Wymaga zgody`); place them beside headings or row titles instead of adding full-width explanatory rows or dangling text below indicators. Group related controls for one task into a clear options surface; distinguish global settings from settings that apply only to the selected month. Long option dialogs should scroll within their content area. Reuse the shared components (`TendHeroCard`, `AllocationBar`, `CategoryRow`, `LedgerEntryRow`, `TendBadge`, `TendSectionHeader`, `TendDialog`) instead of re-implementing them per screen: the read-only and editable category row are the same component with different callbacks, and dropdown affordances use one chevron glyph (`⌄`). When adding a new pattern, first check whether an existing Tend component can express it; if a new pattern is justified, update these guidelines and the shared tokens alongside it.
