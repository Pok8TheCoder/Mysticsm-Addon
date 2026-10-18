$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$nested = "d:\Minecraft\tensuraAdone\temp\nested"
$cp = @(
    "C:\Users\pok8p\AppData\Roaming\PrismLauncher\libraries\net\minecraft\client\1.21.1\client-1.21.1-official.jar",
    "C:\Users\pok8p\AppData\Roaming\PrismLauncher\libraries\net\neoforged\neoforge\21.1.259\neoforge-21.1.259-universal.jar",
    "C:\Users\pok8p\AppData\Roaming\PrismLauncher\libraries\net\neoforged\fancymodloader\loader\4.0.45\loader-4.0.45.jar",
    "C:\Users\pok8p\AppData\Roaming\PrismLauncher\libraries\net\neoforged\bus\8.0.5\bus-8.0.5.jar",
    "C:\Users\pok8p\AppData\Roaming\PrismLauncher\libraries\net\neoforged\mergetool\2.0.0\mergetool-2.0.0-api.jar",
    "d:\Minecraft\tensuraAdone\minecraft\mods\geckolib-neoforge-1.21.1-4.9.3.jar",
    "C:\Users\pok8p\AppData\Roaming\PrismLauncher\libraries\com\mojang\brigadier\1.3.10\brigadier-1.3.10.jar",
    "C:\Users\pok8p\AppData\Roaming\PrismLauncher\libraries\org\joml\joml\1.10.8\joml-1.10.8.jar",
    "C:\Users\pok8p\AppData\Roaming\PrismLauncher\libraries\com\mojang\datafixerupper\8.0.16\datafixerupper-8.0.16.jar",
    "d:\Minecraft\tensuraAdone\minecraft\mods\architectury-13.0.11-neoforge.jar",
    "C:\Users\pok8p\AppData\Roaming\PrismLauncher\libraries\io\netty\netty-buffer\4.1.97.Final\netty-buffer-4.1.97.Final.jar",
    "C:\Users\pok8p\AppData\Roaming\PrismLauncher\libraries\io\netty\netty-common\4.1.97.Final\netty-common-4.1.97.Final.jar",
    "C:\Users\pok8p\AppData\Roaming\PrismLauncher\libraries\io\netty\netty-transport\4.1.97.Final\netty-transport-4.1.97.Final.jar",
    "C:\Users\pok8p\AppData\Roaming\PrismLauncher\libraries\org\spongepowered\mixin\0.8.7\mixin-0.8.7.jar",
    "d:\Minecraft\tensuraAdone\minecraft\mods\tensura-neoforge-2.0.1.3.jar",
    "d:\Minecraft\tensuraAdone\minecraft\mods\SmartBrainLib-neoforge-1.21.1-1.16.11.jar",
    "d:\Minecraft\tensuraAdone\minecraft\mods\mysticism-neoforge-2.2.1.jar",
    "$nested\manascore-skill-neoforge-4.0.0.2.jar",
    "$nested\manascore-storage-neoforge-4.0.0.2.jar",
    "$nested\manascore-network-neoforge-4.0.0.2.jar",
    "$nested\manascore-attribute-neoforge-4.0.0.2.jar",
    "$nested\manascore-race-neoforge-4.0.0.2.jar",
    "$nested\manascore-config-neoforge-4.0.0.2.jar",
    "C:\Users\pok8p\AppData\Roaming\PrismLauncher\libraries\it\unimi\dsi\fastutil\8.5.18\fastutil-8.5.18.jar"
) -join ";"
$classes = Join-Path $root "build\classes"
$jar = Join-Path $root "build\mysticism-ultimates-0.1.0.jar"
if (Test-Path $classes) { Remove-Item -Recurse -Force $classes }
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$sources = Get-ChildItem -Recurse (Join-Path $root "src\main\java") -Filter *.java | ForEach-Object { $_.FullName }
javac --release 21 -encoding UTF-8 -proc:none -cp $cp -d $classes @sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if (Test-Path $jar) { Remove-Item -Force $jar }
jar --create --file $jar -C $classes . -C (Join-Path $root "src\main\resources") .
Copy-Item $jar "d:\Minecraft\tensuraAdone\minecraft\mods\mysticism-ultimates-0.1.0.jar" -Force
Write-Output "built $jar"
