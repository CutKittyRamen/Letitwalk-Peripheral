$ErrorActionPreference = 'Stop'

$projectDir = $PSScriptRoot
$srcDir = "$projectDir\src\main\java"
$buildDir = "$projectDir\build"
$classesDir = "$buildDir\classes"

New-Item -ItemType Directory -Force -Path $classesDir | Out-Null

$baseCp = (Get-Content "$projectDir\classpath.txt" -Raw).Trim()
$nfJar = "C:/Users/PC/AppData/Roaming/PrismLauncher/libraries/net/neoforged/neoforge/21.1.249/neoforge-21.1.249-client.jar"
$ccJar = "C:/Users/PC/AppData/Roaming/PrismLauncher/instances/1.21.1/minecraft/mods/cc-tweaked-1.21.1-forge-1.120.2.jar"
$lwJar = "C:/Users/PC/Downloads/let_it_walk-0.1.4.jar"
$libsJars = Get-ChildItem -Path "$projectDir\libs" -Filter "*.jar" | ForEach-Object { $_.FullName }

$fullCp = @($nfJar, $ccJar, $lwJar) + $libsJars + $baseCp.Split(';') -join ';'
$javaFiles = Get-ChildItem -Path $srcDir -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }

$argFile = "$buildDir\javac.args"
$argsList = @("-proc:none", "-encoding", "UTF-8", "-d", $classesDir, "-cp", $fullCp) + $javaFiles
Set-Content -Path $argFile -Value $argsList

Write-Host "Compiling $($javaFiles.Count) Java files..."
& javac ("@" + $argFile)
if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation successful! Output in $classesDir"
} else {
    Write-Error "Compilation failed!"
}
