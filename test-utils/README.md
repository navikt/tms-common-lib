# tms.common.lib.testutils




## externalServiceSetup
Enklere oppsett av eksterne tjenester for bruk i API-tester

```kotlin
testApplication {
         initExternalServices(
                OppfolgingRoute(false),
                PdlRoute(fødselssår = 1960)
            )
    }
```


### Routeprovider
Kan brukes til å sette opp hvilken som helst respons fra hvilken som helst metode

```kotlin
class OppfolgingRoute(private val underOppfølging: Boolean = false, val ovverideContent: String? = null) :
    RouteProvider(path = "api/niva3/underoppfolging", routeMethodFunction = Routing::get) {
    override fun content(): String = ovverideContent ?: """
        {
          "underOppfolging": $underOppfølging
        }
    """.trimIndent()
}
```

### GraphQlRouteProvider
Setter opp en post rute som svarer på graphql format
```
{
    data: {.....},
    errors: {...}
}
```
Eksempel:
```kotlin
class PdlRoute(
    fødselsdato: String = "1978-05-05",
    fødselssår: Int = 1978,
    errorMsg: String? = null
) :
    GraphQlRouteProvider(errorMsg = errorMsg, path = "pdl/graphql") {
    override val data: String = if (errorMsg == null) """
         {
           "hentPerson": {
      "foedsel": [
        {
          "foedselsdato": "$fødselsdato",
          "foedselsaar": $fødselssår
        }
      ]
    }
         }
    """.trimIndent() else "{}"
}
```