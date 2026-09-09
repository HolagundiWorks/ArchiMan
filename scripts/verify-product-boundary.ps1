$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $repositoryRoot "app/src/main/java"
$manifestPath = Join-Path $repositoryRoot "app/src/main/AndroidManifest.xml"
$buildFile = Join-Path $repositoryRoot "app/build.gradle.kts"
$portalFile = Join-Path $sourceRoot "com/example/portal/LocalPortalServer.kt"

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

Write-Output "Measurement, rate-book, and authenticated local Wi-Fi workspace product boundary verification passed."
