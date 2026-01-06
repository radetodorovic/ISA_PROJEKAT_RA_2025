<#
  install-and-build.ps1
  Preuzima lokalnu kopiju Apache Maven-a u .mvn/ ako mvn nije instaliran u sistemu,
  i pokreće `mvn -DskipTests package` koristeći lokalni mvn ili sistemski mvn.

  Pokretanje (PowerShell):
    .\install-and-build.ps1

  Ako želiš samo preuzeti Maven bez pokretanja build-a:
    .\install-and-build.ps1 -RunBuild:$false
#>

param(
    [switch]$RunBuild = $true
)

function Download-Maven([string]$version, [string]$destZip) {
    $url = "https://archive.apache.org/dist/maven/maven-3/$version/binaries/apache-maven-$version-bin.zip"
    Write-Host "Downloading Maven $version from $url ..."
    Invoke-WebRequest -Uri $url -OutFile $destZip -UseBasicParsing
}

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
# pokušaj da pronađemo mvn u PATH
$mvnCommand = $null
try { $mvnCommand = (Get-Command mvn -ErrorAction Stop).Source } catch { }
if (-not $mvnCommand) {
    Write-Host "mvn nije pronađen u PATH. Preuzimam lokalnu kopiju Maven-a u .mvn/ ..."
    $version = "3.8.8"
    $mvnDir = Join-Path $projectRoot ".mvn"
    if (-not (Test-Path $mvnDir)) { New-Item -ItemType Directory -Path $mvnDir | Out-Null }
    $zipPath = Join-Path $mvnDir "apache-maven-$version-bin.zip"
    $extractDir = Join-Path $mvnDir "apache-maven-$version"
    if (-not (Test-Path $extractDir)) {
        Download-Maven $version $zipPath
        Write-Host "Extracting $zipPath ..."
        Expand-Archive -Path $zipPath -DestinationPath $mvnDir -Force
    }
    $mvnExe = Join-Path $extractDir "bin\mvn.cmd"
} else {
    Write-Host "Pronađen sistemski mvn: $mvnCommand"
    $mvnExe = $mvnCommand
}

if ($RunBuild) {
    Write-Host "Pokrećem build: $mvnExe -DskipTests package"
    & $mvnExe -DskipTests package
}
