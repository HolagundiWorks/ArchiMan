param([string]$Destination = (Join-Path $PSScriptRoot '..\.signing'))
$ErrorActionPreference = 'Stop'
$signingDirectory = [IO.Path]::GetFullPath($Destination)
if (Test-Path -LiteralPath $signingDirectory) { throw 'Signing directory already exists. Existing keys are never replaced.' }
New-Item -ItemType Directory -Path $signingDirectory | Out-Null
$identity = [Security.Principal.WindowsIdentity]::GetCurrent().User
$acl = New-Object Security.AccessControl.DirectorySecurity
$acl.SetAccessRuleProtection($true, $false)
$acl.AddAccessRule((New-Object Security.AccessControl.FileSystemAccessRule($identity, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')))
Set-Acl -LiteralPath $signingDirectory -AclObject $acl
$random = New-Object byte[] 36
[Security.Cryptography.RandomNumberGenerator]::Fill($random)
$secret = [Convert]::ToBase64String($random)
$keyFile = Join-Path $signingDirectory 'archiman-release.jks'
$env:ARCHIMAN_NEW_SIGNING_SECRET = $secret
try {
    $keytool = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\keytool.exe' } else { 'keytool.exe' }
    & $keytool -genkeypair -keystore $keyFile -storetype JKS -alias archiman-production -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 10000 -dname 'CN=ArchiMan Production, OU=Local owner release' -storepass:env ARCHIMAN_NEW_SIGNING_SECRET -keypass:env ARCHIMAN_NEW_SIGNING_SECRET
    if ($LASTEXITCODE -ne 0) { throw 'Key generation failed. Do not reuse this directory; inspect it before retrying.' }
    $normalized = $keyFile.Replace('\','/')
    [IO.File]::WriteAllText((Join-Path $signingDirectory 'release.properties'), "storeFile=$normalized`nstorePassword=$secret`nkeyAlias=archiman-production`nkeyPassword=$secret`n")
    Write-Output "Production signing identity created in $signingDirectory. Back up this private directory offline. Passwords were not printed."
} finally {
    Remove-Item Env:\ARCHIMAN_NEW_SIGNING_SECRET
    [Array]::Clear($random,0,$random.Length)
    $secret = $null
}
