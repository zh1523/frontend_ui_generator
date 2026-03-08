$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$sourceDir = Join-Path $projectRoot "diagrams\mermaid"
$targetDir = Join-Path $projectRoot "figures"
$krokiUrl = "https://kroki.io/mermaid/svg"

New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

$files = Get-ChildItem -Path $sourceDir -Filter *.mmd | Sort-Object Name
if (-not $files) {
    throw "No Mermaid source files were found in $sourceDir"
}

foreach ($file in $files) {
    $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8
    $outFile = Join-Path $targetDir ($file.BaseName + ".svg")
    Invoke-WebRequest `
        -Uri $krokiUrl `
        -Method Post `
        -ContentType "text/plain; charset=utf-8" `
        -Body $content `
        -OutFile $outFile
    Write-Output "Rendered $($file.Name) -> $outFile"
}
