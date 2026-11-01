$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

$sources = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName }
if (-not $sources) {
    throw "No Java source files found."
}

javac -d out $sources
java -cp "out;lib/*" com.attendance.Main .
