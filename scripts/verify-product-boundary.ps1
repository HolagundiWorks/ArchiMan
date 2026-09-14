$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $repositoryRoot "app/src/main/java"
$manifestPath = Join-Path $repositoryRoot "app/src/main/AndroidManifest.xml"
$buildFile = Join-Path $repositoryRoot "app/build.gradle.kts"
$portalFile = Join-Path $sourceRoot "com/example/portal/LocalPortalServer.kt"
$databaseFile = Join-Path $sourceRoot "com/example/data/local/AppDatabase.kt"
$entityFile = Join-Path $sourceRoot "com/example/data/local/entity/Entities.kt"

$prohibitedPattern = '(rate[_ -]?book|ratebook|\brate\b|\brates\b|\bbill\b|\bbills\b|billing|invoice|\bamount\b|payment|retention)'
$activeSourceFiles = Get-ChildItem $sourceRoot -Recurse -File -Filter *.kt |
    Where-Object {
        # Historical migrations retain the old identifiers so schema 14-21
        # databases can be upgraded without losing measurement rows.
        $_.FullName -ne $databaseFile -and
        $_.FullName -notlike '*\ui\theme\*'
    }
$prohibitedMatches = $activeSourceFiles | Select-String -Pattern $prohibitedPattern -CaseSensitive:$false
if ($prohibitedMatches) {
    $prohibitedMatches | ForEach-Object { Write-Error "$($_.Path):$($_.LineNumber): $($_.Line.Trim())" }
    throw "Commercial rate, amount, billing, or payment functionality was found outside historical migration compatibility."
}

$entities = Get-Content $entityFile -Raw
if ($entities -match $prohibitedPattern) {
    throw "The current Room entity model still contains commercial data."
}

$database = Get-Content $databaseFile -Raw
$requiredCommercialRemovalControls = @(
    'const val DATABASE_SCHEMA_VERSION = 25',
    'MIGRATION_21_22',
    'DROP TABLE IF EXISTS `project_rate_book_assignments`',
    'DROP TABLE IF EXISTS `contractor_rate_book_items`',
    'DROP TABLE IF EXISTS `contractor_rate_books`',
    'CREATE TABLE `measurements_quantity_only`'
)
foreach ($control in $requiredCommercialRemovalControls) {
    if (-not $database.Contains($control)) {
        throw "The current schema or schema-22 quantity-only migration is missing: $control"
    }
}

$manifest = Get-Content $manifestPath -Raw
if ($manifest -notmatch 'android\.permission\.INTERNET' -or $manifest -notmatch 'android\.permission\.ACCESS_NETWORK_STATE') {
    throw "The approved local Wi-Fi workspace requires INTERNET and ACCESS_NETWORK_STATE permissions."
}
if ($manifest -notmatch 'android:usesCleartextTraffic="false"') {
    throw "Cleartext traffic must remain disabled."
}

$build = Get-Content $buildFile -Raw
$networkDependencyPattern = '(?m)^\s*implementation\s*\(\s*libs\.(retrofit|converterMoshi|loggingInterceptor|okhttp|firebase)'
if ($build -match $networkDependencyPattern) {
    throw "An active network/cloud dependency was found in the application build."
}

$portal = Get-Content $portalFile -Raw
$requiredPortalControls = @(
    'secureTransport: Boolean = true',
    'HttpOnly; SameSite=Strict',
    'MessageDigest.isEqual',
    'session.principal.role !in setOf("ADMIN", "EDITOR")',
    'LOGIN_BLOCK_MS'
)
foreach ($control in $requiredPortalControls) {
    if (-not $portal.Contains($control)) {
        throw "The local Wi-Fi workspace is missing a required authentication or transport control: $control"
    }
}

$mergedManifest = Join-Path $repositoryRoot "app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml"
if (Test-Path $mergedManifest) {
    $merged = Get-Content $mergedManifest -Raw
    if ($merged -notmatch 'android\.permission\.INTERNET' -or $merged -notmatch 'android\.permission\.ACCESS_NETWORK_STATE') {
        throw "The merged manifest is missing an approved local Wi-Fi workspace permission."
    }
}

Write-Output "Pure Measurement Book and authenticated local Wi-Fi workspace product boundary verification passed."
