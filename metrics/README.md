
# Metrics library


## Ktor Application Installers

### Using default collector registry
```kotlin
installTmsApiMetrics {
    //setup route for scraping, should only be used if the route is not already present. Default: false
    setupMetricsRoute = true
    //Exclude routes from metrics, default: liveness and readiness routes
    ignoreRoutes { route, status ->
        (route == "/ignore" && status == 201) || (route == "/knowndefect" && status == 500)
    }
    //Add custom statusgroup mappings, default: none
    statusGroups {
        "map" belongsTo StatusGroup.IGNORED whenStatusIs HttpStatusCode.BadRequest
    }
}
```

### Using micrometer library
Installation with micrometer has two extra config-options: 

`installMicrometricsPlugin`
Perform installation of the Micrometer plugin, defaults to false.
should be true ONLY IF Micrometer is not already installed

`registry`
Supply a Prometehusregistry to be used as collector, defaults to new instance.
If the default is used, setupMetricsRoute must be set to true
It is nessecary to supply a predefined registry if
1. the application uses R&R(which already has micrometer installed),
2. if micrometrics is used in any other part of the application
3. if the installation of micrometrics is performed by `installTmsMicrometerMetrics`


```kotlin
installTmsMicrometerMetrics {

    registry = registryUsedInApplication
    installMicrometerPlugin = true
    setupMetricsRoute = true
    ignoreRoutes { route, status ->
        (route == "/ignore" && status == 201) || (route == "/knowndefect" && status == 500)
    }
    statusGroups {
        "map" belongsTo StatusGroup.IGNORED whenStatusIs HttpStatusCode.BadRequest
    }
}

```

### Metric content

#### tms_api_call
* `route`: route the call was made to
* `status`: status of response
* `statusgroup`: classification of status based on neccescary actions, see StausGroup in [MetricHelpers](src/main/kotlin/nav/no/tms/common/metrics/MetricHelpers.kt)
* `acr`: authorized sensitivitylevel on request, see Sensitivity in [MetricHelpers](src/main/kotlin/nav/no/tms/common/metrics/MetricHelpers.kt)
