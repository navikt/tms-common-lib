# Observability

Observability-pakken inneholder verktøy for å legge til MDC (Mapped Diagnostic Context) felt i logger for Ktor-applikasjoner. Dette gjør det enklere å filtrere og søke i logger i Grafana.

## Innhold

### ApiMdc Plugin

Et Ktor-plugin som automatisk legger til MDC-felt for `route`, `method` og `domain` i alle requests.

### Domain

En klasse som representerer et domene for logging. Inneholder predefinerte domener:
- `Domain.utkast` - for utkast-relatert innhold
- `Domain.varsel` - for varsel-relatert innhold
- `Domain.microfrontend` - for microfrontend-relatert innhold
- `Domain.none` - for routes/metoder som ikke har noe spesifikt domene (null-verdi). **Dersom du setter `call.mdcDomain = Domain.none` for en request, vil ikke `domain`-feltet legges til i MDC for denne requesten.**

Du kan også opprette egendefinerte domener med `Domain.custom("ditt-domene")`.

## Installasjon

Legg til avhengighet i `build.gradle.kts`:

```kotlin
dependencies {
    implementation("no.nav.tms.common:observability:<versjon>")
}
```

## Brukseksempler

### Grunnleggende bruk - ett domene for hele applikasjonen

Hvis applikasjonen din kun håndterer ett domene:

```kotlin
fun Application.module() {
    install(ApiMdc) {
        applicationDomain = Domain.varsel
    }
    
    routing {
        get("/varsler") {
            // MDC inneholder: route=/varsler, method=GET, domain=varsel
            call.respond(HttpStatusCode.OK)
        }
    }
}
```

### Flere domener (uten felles domene)

Hvis applikasjonen din håndterer flere domener og ikke har ett felles domene, lar du config-blokken være tom:

```kotlin
fun Application.module() {
    install(ApiMdc) { }
    
    routing {
        route("varsel") {
            mdcDomain = Domain.varsel
            get {
                // MDC inneholder: route=/varsel, method=GET, domain=varsel
                call.respond(HttpStatusCode.OK)
            }
        }
        
        route("utkast") {
            mdcDomain = Domain.utkast
            get {
                // MDC inneholder: route=/utkast, method=GET, domain=utkast
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
```

### Domene per request (overstyring)

For finere kontroll kan du sette domene per request i selve metoden:

```kotlin
fun Application.module() {
    install(ApiMdc) { }
    
    routing {
        route("varsel") {
            mdcDomain = Domain.varsel
            get {
                // MDC inneholder: domain=varsel (fra route)
                call.respond(HttpStatusCode.OK)
            }
            post {
                // Overstyr domene for denne spesifikke requesten
                call.mdcDomain = Domain.custom("varsel-opprett")
                // MDC inneholder: domain=varsel-opprett
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
```

### Deaktivere domain-feltet for spesifikke requests

Hvis du ønsker å fjerne `domain`-feltet fra MDC for en spesifikk request (for eksempel på en bestemt route), kan du sette `call.mdcDomain = Domain.none`:

```kotlin
routing {
    get("/uten-domain") {
        call.mdcDomain = Domain.none
        // MDC inneholder: route, method (men ikke domain)
        call.respond(HttpStatusCode.OK)
    }
}
```

### Egendefinert domene

```kotlin
// Opprett et egendefinert domene (4-15 tegn, kun småbokstaver og bindestrek)
val utbetalingDomain = Domain.custom("utbetaling")

fun Application.module() {
    install(ApiMdc) {
        applicationDomain = utbetalingDomain
    }
    // ...
}
```

## Konfigurasjon

`ApiMdc` kan konfigureres med følgende:

| Egenskap | Type | Beskrivelse |
|----------|------|-------------|
| `applicationDomain` | `Domain?` | Standard domene for hele applikasjonen |

Du kan overstyre domenet for en route med `mdcDomain` på `Route`, og for en spesifikk request med `call.mdcDomain` – uten ekstra konfigurasjon.

## MDC-felt

Pluginen legger automatisk til følgende felt i MDC:

| Felt | Beskrivelse |
|------|-------------|
| `route` | Request URI (f.eks. `/api/varsler`) |
| `method` | HTTP-metode (GET, POST, etc.) |
| `domain` | Domenet requesten tilhører. **Feltet utelates dersom `call.mdcDomain = Domain.none` er satt for requesten.** |

Feltene fjernes automatisk etter at responsen er sendt.

---

## Eksempler loggsøk

1. Gå til https://grafana.nav.cloud.nais.io/a/grafana-lokiexplore-app/explore
2. Velg riktig datasource og filtrer på label `service_namespace="min-side"`
   !["Grafana datasource og label filter"](./img/loggstart.png)

3. Filtrer på custom felter i "Fields" seksjonen under labels
   !["Grafana fields filter"](./img/filtreringfields.png)
