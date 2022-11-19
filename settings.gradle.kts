rootProject.name = "melijn-kommons"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}

include(
    "annotation-processor",
    "annotation-processor-redgres",
    "kommons",
    "kord-kommons",
    "annotation-processor-kordex",
    "redgres-kommons",
    "annotation-processor-tests"
)
