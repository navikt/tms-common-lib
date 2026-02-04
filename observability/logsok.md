# Søke og filtrere logger for team min side sine tjenester

## custom felter
- `minside_id`: unik id for innholdet (f.eks varselId for varsler, utkastId for utkast)
- `contenttype`: type innhold (f.eks varsel, utkast, microfrontend, api kall)
- `produced_by`: produsent av innholdet

## Eksempler loggsøk

1. Gå til https://grafana.nav.cloud.nais.io/a/grafana-lokiexplore-app/explore
2. Velg riktig datasource og filtrer på label `service_namespace="min-side"`
!["Grafana datasource og label filter"](./img/loggstart.png)

3. Filtrer på custom felter i "Fields" seksjonen under labels
!["Grafana fields filter"](./img/filtreringfields.png)