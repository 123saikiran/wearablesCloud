# AI API Explorer & Documentation Generator

Upload an OpenAPI/Swagger JSON spec and get AI-generated, human-friendly
documentation: per-endpoint descriptions, example requests/responses, Java
Spring Boot client snippets, an authentication explainer, and a Postman
collection — exportable as Markdown or PDF.

```
React frontend (Vite, Tailwind)
  -> upload OpenAPI JSON
  -> POST /api/specs                (Spring Boot backend)
  -> OpenApiParserService (Jackson) -> ParsedSpec (in-memory, keyed by UUID)
  -> per-endpoint: PromptBuilderService -> AiDocGenerationClient
       (OpenAI Chat Completions | GitHub Models, chosen by `ai.provider`)
  -> GeneratedEndpointDoc per endpoint
  -> MarkdownRenderService -> Markdown string
  -> PdfExportService (flexmark -> HTML -> openhtmltopdf) -> PDF bytes
  -> PostmanCollectionService -> Postman v2.1 JSON (no AI needed)
       |
       v
Frontend renders per-endpoint docs; Export Markdown / PDF / Postman buttons trigger downloads
```

## Layout

```
api-doc-generator/
├── backend/                 # Java 21, Spring Boot 3, Maven (+ wrapper)
│   ├── src/main/java/com/wearablescloud/apidocgen/
│   │   ├── config/          # AI provider + REST client + CORS config
│   │   ├── controller/      # SpecController, DocGenerationController, ExportController, HealthController
│   │   ├── service/         # parsing, prompt building, orchestration, exports
│   │   ├── ai/              # AiDocGenerationClient + OpenAI/GitHub Models implementations
│   │   ├── model/           # parsed-spec domain objects
│   │   ├── dto/             # REST request/response payloads
│   │   └── exception/       # exceptions + global handler
│   ├── src/main/resources/  # application.yml, prompt templates
│   ├── src/test/            # service-layer unit tests
│   └── sample-specs/        # petstore.json, for local testing and demos
└── frontend/                 # React (Vite) + Tailwind
    └── src/
        ├── api/              # apiDocClient.js - fetch wrapper for the backend
        └── components/       # upload form, endpoint list, doc view, export buttons
```

## Setup

Requires Java 21 and Node 20 (no local Maven install needed — use `./mvnw`).

The backend calls an LLM to generate documentation, so you need credentials
for one of two supported providers, selected via `ai.provider`:

```bash
# OpenAI (default)
export AI_PROVIDER=openai
export OPENAI_API_KEY=sk-...

# or GitHub Models
export AI_PROVIDER=github-models
export GITHUB_TOKEN=ghp_...   # a token with Models access
```

See `backend/src/main/resources/application-local.yml.example` for the full
list of overridable settings (model names, base URLs). **Never commit real
secrets** — only the `.example` files are tracked; export env vars instead.
If a key/token is missing, the backend still starts (logging a warning) so
`./mvnw verify` stays green without credentials, but the first AI call fails
with a clear error until one is set.

The frontend reads the backend's URL from `frontend/.env.example` — copy it
to `.env` (gitignored) if you need to point at something other than
`http://localhost:8080`.

## Local development

```bash
# Backend - runs on :8080
cd api-doc-generator/backend
./mvnw spring-boot:run

# Frontend - runs on :5173
cd api-doc-generator/frontend
npm install
npm run dev
```

Manual verification, once the backend is running with a provider configured:

```bash
curl -s localhost:8080/api/health
SPEC_ID=$(curl -s -F "file=@backend/sample-specs/petstore.json" localhost:8080/api/specs | jq -r .specId)
curl -s localhost:8080/api/specs/$SPEC_ID/endpoints | jq .
OP_ID=$(curl -s localhost:8080/api/specs/$SPEC_ID/endpoints | jq -r '.[0].operationId')
curl -s -X POST localhost:8080/api/specs/$SPEC_ID/endpoints/$OP_ID/generate | jq .
curl -s -X POST localhost:8080/api/specs/$SPEC_ID/generate-all | jq '.| length'
curl -s localhost:8080/api/specs/$SPEC_ID/export/markdown -o /tmp/api-docs.md
curl -s localhost:8080/api/specs/$SPEC_ID/export/pdf -o /tmp/api-docs.pdf
curl -s localhost:8080/api/specs/$SPEC_ID/export/postman | jq '.summary'
```

Or open `http://localhost:5173`, upload `backend/sample-specs/petstore.json`,
and click through the endpoint list, generation, and export buttons.

Backend unit tests (parser, prompt builder, Postman/Markdown export - none
need a live AI key) run with:

```bash
cd api-doc-generator/backend && ./mvnw test
```

## Notes / limitations

- Stateless and in-memory: uploaded specs and generated docs live only for
  the life of the backend process, keyed by a UUID handed back on upload.
- `generate-all` calls the AI provider once per endpoint, sequentially - slow
  for large specs; not parallelized or rate-limit-aware in this version.
- `$ref` resolution covers `components.schemas` one level deep; it does not
  follow external file references.
- No authentication/login on the tool itself - it's a single-user local dev
  utility, not a hosted multi-tenant service.
- PDF styling is intentionally basic (headings, code blocks, tables).
