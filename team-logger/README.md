# Team-logger

Team logs tillater apper å logge sensitiv informasjon på en måte der kun team-medlemmer kan se loggmeldingene. Dette erstattet gammel secureLog-config i 2025.

### Builder

Dette biblioteket tilbyr en team-logs builder som sjekker at logger-kontekst er satt opp riktig og gir en KLogger som ruter meldinger til team-logs.

Denne kan brukes slik:

```kotlin
val teamLog = TeamLogs.logger { }

teamLog.info { "Sensitiv info" }

```

Dersom team-logs config ikke er inkludert (se under) vil det kastes en `TeamLogggerNotIncludedException`.

Hvis en ønsker å trygt benytte teamlogs fra en kontekst der en ikke kan endre `logback.xml` direkte (f. eks. fra et bilbiotek) kan en gjøre:

```kotlin
val teamLog = TeamLogs.logger(failSilently = true) { }

teamLog.info { "Sensitiv info fra lib" }


```
Dersom bruker av biblioteket ikke har inkludert `team-logs.xml` logges det bare en advarsel, og meldinger til loggeren blir ignorert.

### Oppsett av logback.xml

Dette biblioteket legger ved en logback-resurs som må inkluderes i `logback.xml` som følger:

```xml
<configuration>
    <include resource="team-logs.xml"/>
    
    ...
</configuration>
```

### Oppdatere nais-yaml

For å kunne rute meldingene ut av appen må følgende legges til i manifestet:

```yaml
...
accessPolicy:
  outbound:
    rules:
      - application: logging
        namespace: nais-system
```
