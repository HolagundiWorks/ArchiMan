$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $repositoryRoot "app/src/main/java"
$manifestPath = Join-Path $repositoryRoot "app/src/main/AndroidManifest.xml"
$buildFile = Join-Path $repositoryRoot "app/build.gradle.kts"

$prohibitedPattern = '\b(bill|bills|billing|invoice|invoices|payment|payments|retention)\b'
$activeSourceFiles = Get-ChildItem $sourceRoot -Recurse -File -Filter *.kt |
    Where-Object {
        $_.FullName -notlike '*\data\local\AppDatabase.kt' -and
        $_.FullName -notlike '*\ui\theme\*'
    }
$prohibitedMatches = $activeSourceFiles | Select-String -Pattern $prohibitedPattern -CaseSensitive:$false
if ($prohibitedMatches) {
    $prohibitedMatches | ForEach-Object { Write-Error "$($_.Path):$($_.LineNumber): $($_.Line.Trim())" }
    throw "Billing or payment functionality was found outside the approved measurement and rate-book scope."
}

$manifest = Get-Content $manifestPath -Raw
if ($manifest -match 'android\.permission\.(INTERNET|ACCESS_NETWORK_STATE)') {
    throw "The source manifest requests a network permission."
}
if ($manifest -notmatch 'android:usesCleartextTraffic="false"') {
    throw "Cleartext traffic must remain disabled."
}

$build = Get-Content $buildFile -Raw
$networkDependencyPattern = '(?m)^\s*implementation\s*\(\s*libs\.(retrofit|converterMoshi|loggingInterceptor|okhttp|firebase)'
if ($build -match $networkDependencyPattern) {
    throw "An active network/cloud dependency was found in the application build."
}

$mergedManifest = Join-Path $repositoryRoot "app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml"
if (Test-Path $mergedManifest) {
    $merged = Get-Content $mergedManifest -Raw
    if ($merged -match 'android\.permission\.(INTERNET|ACCESS_NETWORK_STATE)') {
        throw "The merged debug manifest requests a network permission."
    }
}

Write-Output "Measurement and rate-book product boundary verification passed."
