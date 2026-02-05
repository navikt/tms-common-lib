# Observability

Hensikt: sørge for en helhetlig og sporbar logging fra team min side sine tjenester

| funksjon              | felter                                     |
|-----------------------|--------------------------------------------|
| witMinSideLoggContext | `minside_id`, `contenttype`, `produced_by` |
| withMinSideApiContex  | `route`, `contentype`                      |

## Bruke bilioteket

### Legge på feltene i MDC

```
// Med primitive verdier
withMinSideLoggContext(id, Contenttype.varsel, someteam) { function() }

// Objekt (for mer lesbar kode, eksempel for varsler)

val JsonMessage.context: MinSideLoggContext
    get() = object : MinSideLoggContext {
        override val minsideId: String get() = this["varselId"].asText()
        override val contentType: Contenttype get() = Contenttype.varsel
        override val producedBy: String get() = "team"
        ovverride val extraFields: Map<String, String> get() = mapOf(
            "some_field" to this["some_field"].asText(),
            "action" to this["action"].asText()
        )
    }
withMinSideLoggContext(JsonMessage.cotext) { doTheThings() }
  

 ``` 

#### Hva skal jeg bruke som id?

Hva id-en skal være er litt avhengig av innholdstype. I varsler bruker vi varselId som er unikt,
mens mikrofrontender ikke har noen id som lar oss følge løpet til en spesifik enabling/disabling uten å identifisere
bruker, da gir det mer
mening å bruke id-en til microfrontenen <br/>
NB! Ikke sensitive verdier som f.eks fødselsnummer.

## Søke og filtrere logger for team min side sine tjenester

### custom felter

- `minside_id`: unik id for innholdet (f.eks varselId for varsler, utkastId for utkast)
- `contenttype`: type innhold (f.eks varsel, utkast, microfrontend, api kall)
- `produced_by`: produsent av innholdet

### Eksempler loggsøk

1. Gå til https://grafana.nav.cloud.nais.io/a/grafana-lokiexplore-app/explore
2. Velg riktig datasource og filtrer på label `service_namespace="min-side"`
   !["Grafana datasource og label filter"](./img/loggstart.png)

3. Filtrer på custom felter i "Fields" seksjonen under labels
   !["Grafana fields filter"](./img/filtreringfields.png)

## Oppdateringer v2:

- Mer rigid struktur på feltene i MDC for å tvinge konformitet på tvers av applikasjoner
- Mer bruk av compileOnly avhengigheter for å slanke ned pakkestørrelse
- Bedre dokumentasjon av bruk i logger


