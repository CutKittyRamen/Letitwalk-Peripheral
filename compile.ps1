$ErrorActionPreference = 'Stop'
$srcDir = "D:\cc\letitwalk_cc\src\main\java"
$outDir = "D:\cc\letitwalk_cc\build\classes"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$lines = Get-Content 'd:\cc\letitwalk_work\javac_dbs.args'
$rawCp = $lines[6].Trim('"')

# Filter out vanilla minecraft jars
$filteredEntries = $rawCp.Split(';') | Where-Object { 
    $_ -and -not ($_ -match 'com[\\/]mojang[\\/]minecraft')
}

$nfJar = "C:/Users/PC/AppData/Roaming/PrismLauncher/libraries/net/neoforged/neoforge/21.1.249/neoforge-21.1.249-client.jar"
$ccJar = "C:/Users/PC/AppData/Roaming/PrismLauncher/instances/FuturePort/minecraft/mods/cc-tweaked-1.21.1-forge-1.120.2.jar"
$lwJar = "C:/Users/PC/Downloads/let_it_walk-0.1.4.jar"
$libsJars = Get-ChildItem -Path "D:\cc\letitwalk_cc\libs" -Filter "*.jar" | ForEach-Object { $_.FullName }

$fullCp = @($nfJar, $ccJar, $lwJar) + $libsJars + $filteredEntries -join ';'

$javaFiles = Get-ChildItem -Path $srcDir -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }

$argFile = "D:\cc\letitwalk_cc\build\javac.args"
$argsList = @("-proc:none", "-encoding", "UTF-8", "-d", $outDir, "-cp", $fullCp) + $javaFiles
Set-Content -Path $argFile -Value $argsList
& javac ("@" + $argFile)
Write-Output "Javac exit: $LASTEXITCODE"
