# Tasting Notes — Implementation Plan

## Goal

Add **tasting notes** to a tea's detail page, loaded into the DB from **JSON files** (like tea info)
and rendered per the existing mockup (`tea-view.html`, currently the hard-coded `#tea-reviews`
section). Each note has a star rating, a date, and prose; the page also shows an aggregate rating
summary (count, average, per-star distribution).

## Naming (terminus technicus)

The domain concept is **`TastingNote`** — the term MeiLeaf uses, and consistent with this repo's
convention of adopting MeiLeaf vocabulary (cf. **SCOPE** in `webapp.instructions.md`, which the repo
deliberately does not "fix"). The rename is **full and permanent** — code, DB table, JSON file, CSS
classes, and user-facing copy all say "tasting note(s)", not "review". The mockup's "Reviews"
wording/`.review*` classes are updated as part of this work.

Ownership still uses a **`user_id` FK** (implicitly the single existing user for now, resolved by
`USER_EMAIL` like teas); the name is unrelated to ownership — multi-user ownership stays possible
later. The mockup's four distinct reviewers are a future, multi-user concern; for now every note
renders with the one owner's name/avatar (see UX note in the read-side section).

Class/table vocabulary:

| Role | Name |
| --- | --- |
| Domain aggregate | `TastingNote` (+ `Rating` value object) |
| Write use case | `ReplaceTeaTastingNotes` + `ReplaceTeaTastingNotesCommand` |
| Domain→entity mapper | `TastingNoteMapper` |
| Ingest JSON DTO / mapper | `TastingNoteRecord` / `TastingNoteRecordMapper` |
| Ingest CLI + error | `UploadTastingNotes` / `CannotLoadTastingNotesException` |
| JPA entity / repository | `TastingNoteEntity` / `TastingNoteRepository` |
| Read models | `TastingNoteItem` (one note row) + `RatingSummary` (aggregate) |
| DB table | `TastingNotes` |
| JSON file | `teas/NN/tasting-notes.json` |
| Page section id | `#tea-tasting-notes` |

## Current state (important)

- Mockup commit `404705f` **hard-codes** 4 reviews in `tea-view.html` and updated `TeaViewIT` to
  assert that static HTML. `main` is green today with *fake* data — Increment 1 must swap it for
  real, dynamically-rendered data (and assert the fake names are gone) without going red.
- The DB schema is **generated from JPA entity metadata** (DDL generation; README "Set Up the
  Database"). Tests run on H2 with `ddl-auto=create-drop` + `data.sql`. A new entity ⇒ a new table
  automatically; existing tables are untouched.
- CI (`.github/workflows/deploy.yml`) builds `-DskipTests` and deploys every push to `main`. So the
  new `TastingNotes` table **must exist in PROD before the Increment-1 deploy**, and the always-green
  bar is enforced only locally.

## Domain model

- A **`TastingNote`** belongs to one **Tea** and is owned by a **user** (`user_id`, the single user
  for now). Stored fields: `rating` (half-star, 0.0–5.0), `tastedOn` (date), `body` (single
  plain-text column; paragraphs separated by blank lines).
- **Author display is derived from the owning user** on the read side — display name
  (first name + last initial), avatar initials, avatar colour — **not** stored, **not** in JSON.
  With one user, colour selection is trivial (one fixed palette entry); a deterministic multi-colour
  palette is deferred until multi-user ownership exists (**YAGNI now**).
- The **`RatingSummary`** (count, average, per-star distribution) is *computed* on the read side,
  **not** stored. The distribution has **six rows, 5★ down to 0★**; each note rounds half-up into
  exactly one bucket (4.5 ⇒ 5★, 0.0 ⇒ 0★), so bars always sum to the note count. Average is computed
  from integer half-star totals and formatted HALF_UP to one decimal with `Locale.ENGLISH`. **Empty
  state is distinct from 0.0** (0.0 is a valid rating): absence is modelled by `count == 0` /
  an empty/`Optional` average, never a 0.0 average.
- **Ordering:** notes render **newest first** (`tastedOn DESC`, `id` as a stable tie-break).

## JSON schema

`teas/NN/tasting-notes.json` — a JSON array, co-located with the tea (see Decisions #3). No
owner/author field (implicitly the single user, like tea `info.json`). Kept separate from
`info.json` because tea ingest (`UploadNewTeas`) is incremental/add-only (resumes by max id, skips
existing tea dirs) whereas tasting-note ingest is idempotent replace-per-tea (Decisions #5).

```json
[
  {
    "rating": 5.0,
    "date": "2026-07-21",
    "body": [
      "Clean, layered and quietly energizing. Floral top notes drift into a sweet mineral finish that lingers well past the swallow.",
      "One of my favourite sessions this year — remarkably consistent from the first steep to the last."
    ]
  },
  {
    "rating": 4.5,
    "date": "2026-06-08",
    "body": [
      "Thick, almost syrupy texture with ripe stone-fruit notes from the very first steep.",
      "Best balance for me was 5g / 100ml starting around 20 seconds."
    ]
  }
]
```

- `rating`: number, 0.0–5.0 in 0.5 steps (0 stars allowed). Parsed as `BigDecimal`, converted **once**
  to an integer 0–10 half-stars by `Rating`; a non-half-step or out-of-range value is rejected.
- `date`: ISO `yyyy-MM-dd` → `LocalDate` (needs Jackson JavaTimeModule — see Increment 2 note);
  displayed as `d MMM yyyy` in `Locale.ENGLISH` (e.g. "21 Jul 2026").
- `body`: array of paragraphs (≥1) for readable/diff-friendly authoring; **joined with a blank line**
  into the single `body` column on ingest, and **split back on blank lines** to render one `<p>`
  each (split trims each segment and drops empties, so stray/CRLF blank lines can't create phantom
  paragraphs).
- The example prose above is **illustrative/test-only** — production `tasting-notes.json` must be
  real content authored by the owner, not the mockup's relabeled "Ada K." copy.

## DB schema (one new table, JPA-generated; existing tables unchanged)

- `TastingNotes(id PK identity, tea_id FK→Teas NOT NULL, user_id FK→Users NOT NULL,
  rating_half_stars SMALLINT NOT NULL, tasted_on DATE NOT NULL, body TEXT NOT NULL)`.
  - `body` uses an explicit `@Column(columnDefinition = "TEXT")` (or a large VARCHAR) — otherwise JPA
    defaults to `VARCHAR(255)`.
  - The `0–10` range invariant lives in the `Rating` VO (domain). A DB `CHECK` and composite index
    are **not** added now (YAGNI for a bounded personal dataset); a plain `tea_id` index is enough
    and is what the read query filters on.
- **PROD migration:** regenerate `ddl-schema.sql` and apply the new `CREATE TABLE` (with its FK
  statements) **before** the Increment-1 deploy, since CI ships on push with `-DskipTests`.

## Read-side wiring (no EAGER collection on `TeaEntity`)

Reviews/notes are **not** mapped as a collection on `TeaEntity`. `TeaEntity` already has three EAGER
collections (types, images, tags); adding an EAGER, text-heavy notes collection would bloat every
full `TeaEntity` load — `TeaQueryController.teaView`'s `findById` **and** `UploadNewTeas`'s
`teaRepository.findAll()` (used only to compute max id) — via join multiplication. (The index page is
safe regardless: `TeaQueryRepository` projects scalar `Tuple`s, not entities.)

Instead: `TastingNoteRepository.findByTeaIdOrderByTastedOnDesc(teaId)`. The controller fetches the
notes, builds `List<TastingNoteItem>` + `RatingSummary`, and adds them to the model (either passed
into an overloaded `TeaDetail.from(tea, notes)` or as separate model attributes).

## Increments (each self-contained, fully tested, green & deployable)

### Increment 1 — Read side + dynamic rendering

Write layer untouched; notes are seeded in tests via `TastingNoteRepository`.

- **persistence:** `TastingNoteEntity` (single `body` TEXT column; `@ManyToOne` `tea` + `user`, both
  `optional=false`), `TastingNoteRepository extends JpaRepository` with
  `findByTeaIdOrderByTastedOnDesc`. **No collection added to `TeaEntity`.**
- **query:** `TastingNoteItem` (authorName / initials / avatarClass derived from the owner user;
  ratingValue (0.0–5.0), ratingLabel, dateLabel via `Locale.ENGLISH`; paragraphs = `body` split on
  blank lines, trimmed, empties dropped) + `RatingSummary` (count, `Optional`/nullable averageValue
  + averageLabel, six distribution rows 5★→0★ each with count and `pct`). Owner display-name/initials
  have null-safe fallbacks (never expose email).
- **web:** `TeaQueryController.teaView` loads notes via the repository and adds
  `tastingNotes` + `ratingSummary` to the model.
- **view:** replace the hard-coded `#tea-reviews` block with `#tea-tasting-notes` bound to the model;
  rename `.review*`/`.reviews*` markup + CSS classes to `.tasting-note*`/`.tasting-notes*` (keep the
  generic `.stars*`, `.dist-*`, `.avatar-*` classes); **add the 0★ distribution row** (mockup ships
  only 5★–1★); render a small **empty state** ("No tasting notes yet") when there are no notes. Body
  stays `th:text` (auto-escaped).
- **README:** update the **read-side** of the architecture diagram in this increment.
- **tests:**
  - unit `TastingNoteItemTests` (paragraph split incl. stray/CRLF blank lines, derived name/initials
    + null-name fallback, date label locale, HTML-escaping expectation), `RatingSummaryTests`
    (half-up bucketing incl. the **0★** bucket, average HALF_UP, **empty ≠ 0.0**, singular "1 tasting
    note").
  - integration: rewrite `TeaViewIT` to seed real notes and assert the dynamic output, assert the old
    fake names ("Ada K." …) are **absent**, cover a tea with **no notes** (small empty state), the
    rendered **0★** row, and that a body containing markup is **escaped**.
- **Deploy note:** the `TastingNotes` table must exist in PROD first. The section then renders a small
  **empty state** ("No tasting notes yet") for every tea until Increment 2 loads data.

### Increment 2 — Write side + JSON ingest (populate from files)

- **Prerequisite bug-fix (ships in this increment):** `TeaRecord.loadImages()` loads *every* file
  except `info.json` as an image (`ImageIO.read`), so a co-located `tasting-notes.json` would be read
  as a JPEG and crash `UploadNewTeas`. Fix it to **whitelist image files** (e.g. `.jpg/.jpeg/.png`).
  Regression tests: `TeaRecordTests` + `UploadNewTeasIT` with a `tasting-notes.json` present in a tea
  dir.
- **domain:** `TastingNote` aggregate + `Rating` VO. `TastingNote` holds only `rating`, `tastedOn`
  and `body`; its tea and owner are **context** supplied by the service (mirroring how `Tea` omits
  its owner), not domain fields. Invariants (throw `IllegalArgumentException`): `rating` and
  `tastedOn` non-null, `body` non-blank (≥1 non-blank paragraph after splitting). `Rating` owns the
  0–10 ↔ 0.0–5.0 conversion (range/half-step validated), `.value()`, `.roundedStars()`, `.label()`,
  and an `ofStars(BigDecimal)` factory for the ingest.
- **application:** `ReplaceTeaTastingNotes` (`@Service`) + `ReplaceTeaTastingNotesCommand`
  (`teaId`, `userId` owner, and a `List<NoteData>` — each `NoteData` a rating/date/joined-body carrier,
  paralleling how `AddTeaCommand` carries a tea's fields rather than a built `Tea`) + `TastingNoteMapper`
  (pure domain→entity). **Atomic replace per tea:** the service builds & validates *all*
  `TastingNote` aggregates from the command (mirroring `AddTea` building its `Tea`), resolves owner
  user + validates tea existence, then **deletes existing notes for the tea and `saveAll`s** in **one
  transaction** — a bad note leaves that tea's existing notes intact. (Orchestration lives in the use
  case, not the ingest adapter, mirroring `AddTea`/`TeaMapper`.)
- **ingest:** `TastingNoteRecord` (Jackson JSON contract — rating/date/body, `required` wrapper types
  so a missing rating isn't a silent 0) + `TastingNoteRecordMapper` (join paragraphs, `BigDecimal`
  rating → half-stars) + `UploadTastingNotes` CLI (`@SpringBootApplication`; resolves the single user
  by `USER_EMAIL`; walks tea dirs; **absent `tasting-notes.json` ⇒ skip that tea (leave existing
  notes untouched); `[]` ⇒ clear that tea's notes**) + `CannotLoadTastingNotesException`. Configure
  the `ObjectMapper` with `JavaTimeModule` for `LocalDate` and strict ISO parsing.
  - **Tea resolution:** resolve the tea by folder id with an existence check (abort if no such tea).
    The folder number is only coincidentally equal to the DB `IDENTITY` id, so folder↔DB alignment is
    a **documented assumption** — safe for this single-maintainer, import-in-order dataset.
- **README + instructions:** finish the architecture diagram (write/ingest side: `TastingNote`,
  `ReplaceTeaTastingNotes`, `RatingSummary`/`TastingNoteItem`, `UploadTastingNotes`), the
  "Set Up the Database" steps (new optional `UploadTastingNotes` run; regenerate DDL), and refresh
  the green-baseline test counts in `.github/instructions/webapp.instructions.md`.
- **tests:**
  - unit `TastingNoteTests`, `RatingTests` (range/half-step/conversion), `TastingNoteRecordTests`
    (required fields, ISO date, body array), `TastingNoteRecordMapperTests` (join, rating parse),
    `TastingNoteMapperTests`.
  - integration `UploadTastingNotesIT` (happy path; **idempotent re-run** — twice yields one set, no
    dupes; invalid note ⇒ abort, tea keeps old notes; **absent file ⇒ skip**; **`[]` ⇒ clear**;
    malformed JSON / out-of-range rating rejection), a `ReplaceTeaTastingNotesServiceIT`
    application-service IT (`@Transactional` like the other ITs — a non-transactional variant caused
    H2 cross-test pollution when deleting an `AddTea`-created tea with images, and adds no coverage
    since every failure path resolves before the delete), and the image-loader regression. `TeaViewIT`
    keeps its Increment-1 repository seeding (decoupled precise render assertions; the ingest path is
    covered by `UploadTastingNotesIT`). Add `tasting-notes.json` fixtures under
    `src/test/resources/teas/NN/`.
- **Deploy note:** table already created before Inc 1; here, load data via `UploadTastingNotes`.

*Optional finer split:* 1a note list / 1b summary; 2a bug-fix + domain + application (+unit) / 2b
ingest CLI (+IT).

## Decisions

1. **DECIDED — concept named `TastingNote`** (MeiLeaf terminus technicus), full/permanent rename
   across code, DB, JSON, CSS, UI. Ownership retains `user_id` for future multi-user.
2. **DECIDED — half-star granularity, 0.0–5.0** (0 stars allowed), stored as a half-star integer
   (0–10); `Rating` owns the conversion.
3. **DECIDED — separate `teas/NN/tasting-notes.json`** (co-located), because tea ingest is
   incremental/add-only while tasting-note ingest is idempotent replace-per-tea.
4. **DECIDED — single plain-text `body` column, split on blank lines to render** (no child table).
   JSON `body` array is joined with a blank line on ingest. Rich text (bold/links) is future work —
   would switch this column's content to Markdown (rendered to **sanitized** HTML via `th:utext`),
   with no schema change.
5. **DECIDED — idempotent replace-per-tea**, CLI named `UploadTastingNotes`; each run replaces a
   tea's notes from its file (files are the source of truth). Note ids are IDENTITY and **disposable**
   (they churn on re-import) — never expose or reference them.
6. **DECIDED — store `user_id` (owner)** on each note, mirroring teas.
7. **DECIDED — distribution histogram has six rows, 5★→0★**; each note rounds half-up into exactly
   one bucket, so bars sum to the note count. Template adds the 0★ row the mockup lacks.
8. **DECIDED — date field is `tastedOn` / column `tasted_on`** (the date of the tasting), not an
   ambiguous `date`. Future dates are **not** validated for now (deliberate non-goal).
9. **DECIDED — multiple notes per tea by the one owner are allowed** (the mockup shows several), so
   **no** uniqueness constraint on `(tea_id, user_id)`.
10. **DECIDED — empty state:** when a tea has no notes, render a small **"No tasting notes yet"**
    message (not a hidden section, not a "0 reviews" block).
11. **DECIDED — ingest tea resolution by folder id + existence check** (abort if missing), trusting
    the folder↔DB id alignment as a documented assumption; no `info.json` cross-check and no
    `source_id` column for now.
