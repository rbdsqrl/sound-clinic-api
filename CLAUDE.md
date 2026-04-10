# Simple Hearing Backend — CLAUDE.md

Developer context for AI assistants working on this codebase.

---

## Tech Stack

| Layer       | Technology                        |
|-------------|-----------------------------------|
| Language    | Java 21                           |
| Framework   | Spring Boot 3.3.4                 |
| Build       | Maven                             |
| JSON        | Jackson (via Spring Web)          |

---

## Running Locally

```bash
cd backend
mvn spring-boot:run
```

Server starts on **http://localhost:8080**.

---

## Health Checks

| Method | URL               | Description                              |
|--------|-------------------|------------------------------------------|
| GET    | `/`               | Root ping — `{ "status": "running" }`   |
| GET    | `/health`         | Manual health probe                      |
| GET    | `/actuator/health`| Spring Actuator health (shows details)   |

---

## Page Layout API

### Overview

`GET /page/{pageId}` returns a fully-assembled page layout that both the mobile
app and website can render without any further logic. The response is
self-describing: every section carries a `type` field that tells the client
which component to render.

### Registered Pages

| pageId  | URL          | Assembler class       |
|---------|--------------|-----------------------|
| `home`  | `/page/home` | `HomePageAssembler`   |

Returns `404 Not Found` for any unregistered `pageId`.

### Response Shape

```json
{
  "page_id": "home",
  "meta": {
    "title": "...",
    "description": "..."
  },
  "sections": [
    {
      "id":    "hero",
      "type":  "hero",
      "order": 1,
      "data": { "type": "hero", ... }
    },
    ...
  ]
}
```

### Section Types

Each section's `data` object has a `type` discriminator field.

| `type`              | Java record              | Description                              |
|---------------------|--------------------------|------------------------------------------|
| `hero`              | `HeroData`               | Full-width hero with headline + 2 CTAs   |
| `stats_bar`         | `StatsBarData`           | Row of stat items (value + label)        |
| `services_preview`  | `ServicesPreviewData`    | Grid of service cards with icon keys     |
| `brands`            | `BrandsData`             | List of brand names                      |
| `why_choose_us`     | `WhyChooseUsData`        | Bullet points with icon keys             |
| `cta_banner`        | `CtaBannerData`          | Full-width call-to-action banner         |

#### Icon Keys

`iconKey` fields (in `ServicesPreviewData.ServiceCard` and `WhyChooseUsData.BulletPoint`)
are **semantic tokens**, not platform-specific names. Each client maps them to
its own icon system:

- Mobile (Compose): map in `iconForService()` / icon resolver
- Web: map in a React/Vue icon utility

Current service icon keys: `pediatric`, `sensory`, `stroke`, `swallowing`, `avt`, `cochlear`
Current why-choose-us icon keys: `check_circle`, `star`, `person`, `location`

---

## Package Structure

```
com.simplehearing
├── config/
│   └── JacksonConfig.java          # Global Jackson: snake_case, non-null, ISO dates
├── controller/
│   ├── HealthController.java        # GET /, GET /health
│   └── PageController.java         # GET /page/{pageId}
├── service/
│   └── page/
│       ├── PageService.java         # interface
│       └── PageServiceImpl.java     # resolves assembler by pageId via Spring bean map
├── assembler/
│   └── page/
│       ├── PageAssembler.java       # interface: PageResponse assemble()
│       └── HomePageAssembler.java   # @Component("home") — stub data, TODO for DB
└── dto/
    └── page/
        ├── CtaButton.java
        ├── PageMeta.java
        ├── PageSection.java
        ├── PageResponse.java
        └── section/
            ├── SectionData.java     # sealed interface + @JsonSubTypes registry
            ├── HeroData.java
            ├── StatsBarData.java
            ├── ServicesPreviewData.java
            ├── BrandsData.java
            ├── WhyChooseUsData.java
            └── CtaBannerData.java
```

---

## Adding a New Page

1. Create `assembler/page/YourPageAssembler.java` implementing `PageAssembler`.
2. Annotate it `@Component("yourPageId")`.
3. Build the `PageResponse` in `assemble()` — use the existing section DTOs or add new ones.
4. If you add a **new section type**:
   - Create the record in `dto/page/section/`, implement `SectionData`.
   - Add it to the `permits` clause in `SectionData.java`.
   - Register it in `@JsonSubTypes` in `SectionData.java`.
5. Register the new `pageId` in this file's table above.
6. No changes needed in `PageController` or `PageServiceImpl`.

---

## Stub Pattern & DB Integration Points

All assemblers currently return hardcoded data. Stub methods are marked with
`TODO` comments indicating the repository call that will replace them:

```java
// TODO: replace stub list with ServiceRepository.findAll() once DB is wired
```

When a repository is introduced:
- Inject it into the relevant assembler via constructor injection.
- Replace the stub list with the repository call.
- No other class needs to change.

---

## Jackson Configuration

- **Property naming**: `snake_case` (set globally in `JacksonConfig`)
- **Null fields**: omitted from output (`NON_NULL`)
- **Dates**: ISO-8601 strings (no timestamps)
- **Polymorphism**: `SectionData` uses `@JsonTypeInfo(As.EXISTING_PROPERTY)` —
  the `type` field on each record doubles as the Jackson discriminator. Every
  record must declare `String type()` returning a fixed string matching its
  `@JsonSubTypes.Type(name = "...")`.
