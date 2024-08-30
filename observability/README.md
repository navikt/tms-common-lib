# Observability

Hensikt: sørge for en helhetlig og sporbar logging fra team min side sine tjenester

Legger by default på feltene `minside_id` og `contenttype` i MDC

## Bruke bilioteket

### Legge på feltene i MVC

```
// Standard
withTraceLogging(id, Contenttype.varsel) { doStuff() }
// Med ekstra felter
withTraceLogging(id, Contenttype.varsel, extra, mapOf("custom_field" to "some custom value")) { function() }

```

#### Convenience funksjoner

Det er lagt til convenience funksjoner med noen preutfylte verdier for 3 av contentypene; varsel, microfrontends og
utkast.

```
// Varsler
traceVarsel(id){ doStuffVarselStuff() }
//Utkast
traceUtkast(id){ doUtkastStuff() }
// Microfrontends
fun traceMicrofrontend(id){ doStuff() }

```


#### Hva skal jeg bruke som id?

Hva id-en skal være er litt avhengig av innholdstype. I varsler bruker vi varselId som er unikt,
mens mikrofrontender ikke har noen id som lar oss følge løpet til en spesifik enabling/disabling uten å identifisere
bruker, da gir det mer
mening å bruke id-en til microfrontenen <br/>
NB! Ikke sensitive verdier som f.eks fødselsnummer.

## Eksempler loggsøk:

```
#Ett spesifikt varsel i prod
x_minside_id :"<varselid>"  and cluster:"prod-gcp"
#Alle mikrofrontender i dev
x_contenttype :"microfrontend" and cluster:"dev-gcp"
```

