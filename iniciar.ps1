param([ValidateSet('dev', 'prod')][string]$Perfil = 'dev')
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (Test-Path -LiteralPath '.env') {
    foreach ($linha in Get-Content -LiteralPath '.env') {
        if ($linha -match '^([A-Z][A-Z0-9_]*)=(.*)$' -and $Matches[2].Length -gt 0) {
            [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2].Trim(), 'Process')
        }
    }
}
if (-not $env:JAVA_HOME -and -not (Get-Command java -ErrorAction SilentlyContinue)) {
    $javaCache = Join-Path $env:USERPROFILE '.cache\ecoflux-tools\jdk21'
    $jdk = Get-ChildItem -LiteralPath $javaCache -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
}
if ($env:JAVA_HOME) { $env:PATH = "$env:JAVA_HOME\bin;$env:PATH" }
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Instale o JDK 21 e configure JAVA_HOME.' }
$env:SPRING_PROFILES_ACTIVE = $Perfil
& "$PSScriptRoot\mvnw.cmd" spring-boot:run
exit $LASTEXITCODE
