$ErrorActionPreference = 'Stop'

$projectDir = "D:\cc\letitwalk_cc"
$srcDir = "$projectDir\src\main\java"
$resDir = "$projectDir\src\main\resources"
$buildDir = "$projectDir\build"
$classesDir = "$buildDir\classes"
$stageDir = "$buildDir\stage"

Write-Host "=== 1. Compiling Java sources ==="
New-Item -ItemType Directory -Force -Path $classesDir | Out-Null
New-Item -ItemType Directory -Force -Path $stageDir | Out-Null

$lines = Get-Content 'd:\cc\letitwalk_work\javac_dbs.args'
$rawCp = $lines[6].Trim('"')

# Filter out vanilla minecraft jars
$filteredEntries = $rawCp.Split(';') | Where-Object { 
    $_ -and -not ($_ -match 'com[\\/]mojang[\\/]minecraft')
}

$nfJar = "C:/Users/PC/AppData/Roaming/PrismLauncher/libraries/net/neoforged/neoforge/21.1.249/neoforge-21.1.249-client.jar"
$ccJar = "C:/Users/PC/AppData/Roaming/PrismLauncher/instances/FuturePort/minecraft/mods/cc-tweaked-1.21.1-forge-1.120.2.jar"
$lwJar = "C:/Users/PC/Downloads/let_it_walk-0.1.4.jar"
$libsJars = Get-ChildItem -Path "$projectDir\libs" -Filter "*.jar" | ForEach-Object { $_.FullName }

$fullCp = @($nfJar, $ccJar, $lwJar) + $libsJars + $filteredEntries -join ';'

$javaFiles = Get-ChildItem -Path $srcDir -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }

$argFile = "$buildDir\javac.args"
$argsList = @("-proc:none", "-encoding", "UTF-8", "-d", $classesDir, "-cp", $fullCp) + $javaFiles
Set-Content -Path $argFile -Value $argsList
& javac ("@" + $argFile)
if ($LASTEXITCODE -ne 0) {
    throw "Compilation failed with code $LASTEXITCODE"
}
Write-Host "Compilation successful!"

Write-Host "=== 2. Assembling JAR staging directory ==="
if (Test-Path $stageDir) {
    Remove-Item -Recurse -Force $stageDir
}
New-Item -ItemType Directory -Force -Path $stageDir | Out-Null

# Copy classes
Copy-Item -Recurse -Force "$classesDir\*" $stageDir

# Copy resources
Copy-Item -Recurse -Force "$resDir\*" $stageDir

# Manifest
$manifestFile = "$buildDir\MANIFEST.MF"
$manifestContent = @"
Manifest-Version: 1.0
Implementation-Title: Let it Walk: ComputerCraft Addon
Implementation-Version: 1.0.0
Specification-Title: letitwalk_cc
Specification-Version: 1.0.0
Specification-Vendor: Antigravity
Implementation-Vendor: Antigravity
"@
Set-Content -Path $manifestFile -Value $manifestContent

Write-Host "=== 3. Creating JAR file ==="
$outJar = "$buildDir\letitwalk_cc-1.0.0.jar"
if (Test-Path $outJar) {
    Remove-Item -Force $outJar
}

Set-Location $stageDir
& jar -cfm $outJar $manifestFile *
if ($LASTEXITCODE -ne 0) {
    throw "Jar creation failed with code $LASTEXITCODE"
}
Write-Host "Created JAR: $outJar ($( (Get-Item $outJar).Length ) bytes)"

Write-Host "=== 4. Copying to deployment locations ==="
Copy-Item -Force $outJar "D:\cc\letitwalk_cc-1.0.0.jar"
Write-Host "Copied to: D:\cc\letitwalk_cc-1.0.0.jar"

$instanceMods = "C:\Users\PC\AppData\Roaming\PrismLauncher\instances\1.21.1\minecraft\mods"
if (Test-Path $instanceMods) {
    Copy-Item -Force $outJar "$instanceMods\letitwalk_cc-1.0.0.jar"
    Write-Host "Copied to instance mods folder: $instanceMods\letitwalk_cc-1.0.0.jar"
}

# Also ensure CC: Tweaked is in the instance mods if not already there!
$ccTarget = "$instanceMods\cc-tweaked-1.21.1-forge-1.120.2.jar"
if (-not (Test-Path $ccTarget) -and (Test-Path $ccJar)) {
    Copy-Item -Force $ccJar $ccTarget
    Write-Host "Installed CC: Tweaked into instance mods: $ccTarget"
}

Write-Host "=== BUILD AND DEPLOY COMPLETED SUCCESSFULLY! ==="
