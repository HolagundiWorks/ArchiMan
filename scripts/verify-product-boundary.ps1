$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $repositoryRoot "app/src/main/java"
$manifestPath = Join-Path $repositoryRoot "app/src/main/AndroidManifest.xml"
$buildFile = Join-Path $repositoryRoot "app/build.gradle.kts"

$commercialPattern = '\b(rate|rates|bill|bills|billing|invoice|invoices|payment|payments|amount|price|cost)\b'
$activeSourceFiles = Get-ChildItem $sourceRoot -Recurse -File -Filter *.kt |
    Where-Object {
        $_.FullName -notlike '*\data\local\AppDatabase.kt' -and
        $_.FullName -notlike '*\ui\theme\*'
    }
$commercialMatches = $activeSourceFiles | Select-String -Pattern $commercialPattern -CaseSensitive:$false
if ($commercialMatches) {
    $commercialMatches | ForEach-Object { Write-Error "$($_.Path):$($_.LineNumber): $($_.Line.Trim())" }
    throw "Commercial terminology was found in active product source."
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

Write-Output "Pure Measurement Book boundary verification passed."
