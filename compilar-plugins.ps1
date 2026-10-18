$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$libDir = Join-Path $root "libraries"
if (-not (Test-Path $libDir)) { exit 0 }

$jars = (Get-ChildItem -Path $libDir -Recurse -Filter "*.jar" | Select-Object -ExpandProperty FullName) -join ";"
$javacCmd = Get-Command javac -ErrorAction SilentlyContinue
if (-not $javacCmd) { exit 0 }
$jarExe = Join-Path (Split-Path $javacCmd.Source) "jar.exe"

$pluginsDir = Join-Path $root "plugins"
$updateDir = Join-Path $pluginsDir "update"
New-Item -ItemType Directory -Force -Path $pluginsDir | Out-Null
New-Item -ItemType Directory -Force -Path $updateDir | Out-Null

# 1. Compila PingPlugin
$pingOut = Join-Path $root "src-ping\build"
if (Test-Path $pingOut) { Remove-Item -Recurse -Force $pingOut }
New-Item -ItemType Directory -Force -Path $pingOut | Out-Null
& javac --release 25 -encoding UTF-8 -cp $jars -d $pingOut (Join-Path $root "src-ping\src\main\java\me\servidor\ping\PingPlugin.java")
Copy-Item (Join-Path $root "src-ping\src\main\resources\plugin.yml") -Destination $pingOut -Force
& $jarExe --create --file (Join-Path $updateDir "PingPlugin.jar") -C $pingOut .
try { Copy-Item (Join-Path $updateDir "PingPlugin.jar") -Destination (Join-Path $pluginsDir "PingPlugin.jar") -Force -ErrorAction Stop } catch {}

# 2. Compila TagsPlugin (QuestCodex)
$tagsOut = Join-Path $root "src-tags\build"
if (Test-Path $tagsOut) { Remove-Item -Recurse -Force $tagsOut }
New-Item -ItemType Directory -Force -Path $tagsOut | Out-Null
& javac --release 25 -encoding UTF-8 -cp $jars -d $tagsOut (Join-Path $root "src-tags\src\main\java\me\servidor\tags\TagsPlugin.java")
Copy-Item (Join-Path $root "src-tags\src\main\resources\plugin.yml") -Destination $tagsOut -Force
Copy-Item (Join-Path $root "src-tags\src\main\resources\config.yml") -Destination $tagsOut -Force
& $jarExe --create --file (Join-Path $updateDir "TagsPlugin.jar") -C $tagsOut .
try { Copy-Item (Join-Path $updateDir "TagsPlugin.jar") -Destination (Join-Path $pluginsDir "TagsPlugin.jar") -Force -ErrorAction Stop } catch {}
