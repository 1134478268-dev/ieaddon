param(
    [switch]$NoLaunch
)

$ErrorActionPreference = 'Stop'

$proj   = 'D:\dev\projects\IEAddon'
$ver    = Join-Path $proj 'versions\1.12.2'
$run    = Join-Path $proj 'run-1.12.2'
$jar    = Join-Path $ver 'build\libs\ieaddon-1.0.0.jar'
$modJar = Join-Path $run 'mods\ieaddon-1.0.0.jar'
$log    = 'D:\dev\tmp\build-flow.log'

$env:JAVA_HOME = 'D:\dev\tools\jdk-8'
$env:GRADLE_USER_HOME = 'D:\dev\tools\gradle-home'

Write-Host '=== [1/4] build ===' -ForegroundColor Cyan
Set-Location $ver
# 删掉旧产物强制重打包，但保留 ForgeGradle 的 setup 缓存（比 --rerun-tasks 快一个数量级）
Remove-Item (Join-Path $ver 'build\libs\*.jar') -Force -ErrorAction SilentlyContinue
& .\gradlew.bat build --no-daemon 2>&1 | Tee-Object -FilePath $log | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host 'BUILD FAILED' -ForegroundColor Red
    Select-String -Path $log -Pattern 'error:|\.java:\d+' | Select-Object -First 10 | ForEach-Object { $_.Line }
    exit 1
}

Write-Host '=== [2/4] jar freshness ===' -ForegroundColor Cyan
if (!(Test-Path $jar)) {
    Write-Host 'JAR MISSING' -ForegroundColor Red; exit 1
}
$jarTime = (Get-Item $jar).LastWriteTime
$newestSrc = (Get-ChildItem (Join-Path $ver 'src') -Recurse -File |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1).LastWriteTime
if ($jarTime -lt $newestSrc) {
    Write-Host ("JAR STALE: jar={0}  newest source={1}" -f $jarTime, $newestSrc) -ForegroundColor Red
    exit 1
}
Write-Host ("jar OK: {0} bytes @ {1}" -f (Get-Item $jar).Length, $jarTime) -ForegroundColor Green

Write-Host '=== [3/4] deploy ===' -ForegroundColor Cyan
# 先杀掉占用 jar 的旧游戏，否则 Copy-Item 会失败
Get-Process java -ErrorAction SilentlyContinue |
    Where-Object { $_.Path -like '*jdk-8*' } |
    ForEach-Object { Stop-Process -Id $_.Id -Force }
Start-Sleep -Seconds 3
Copy-Item $jar $modJar -Force
if ((Get-Item $modJar).LastWriteTime -lt $jarTime) {
    Write-Host 'DEPLOY FAILED: mods jar is older than build jar' -ForegroundColor Red
    exit 1
}
Write-Host ("copied to mods: {0}" -f (Get-Item $modJar).LastWriteTime) -ForegroundColor Green

if ($NoLaunch) {
    Write-Host '=== [4/4] launch skipped (-NoLaunch) ===' -ForegroundColor Yellow
    exit 0
}

Write-Host '=== [4/4] launch ===' -ForegroundColor Cyan
Get-Process java -ErrorAction SilentlyContinue |
    Where-Object { $_.Path -like '*jdk-8*' } |
    ForEach-Object { Stop-Process -Id $_.Id -Force }
Start-Sleep -Seconds 3

$latest = Join-Path $run 'logs\latest.log'
if (Test-Path $latest) { Remove-Item $latest -Force -ErrorAction SilentlyContinue }

Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', '启动游戏.bat' -WorkingDirectory $run -WindowStyle Minimized

$deadline = (Get-Date).AddSeconds(90)
$ok = $false
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 5
    if (!(Test-Path $latest)) { continue }
    $tail = Get-Content $latest -Tail 80 -ErrorAction SilentlyContinue
    if ($tail -match 'Game crashed|NoSuchMethodError|Exception in thread') {
        Write-Host 'GAME CRASHED' -ForegroundColor Red
        $tail | Select-String -Pattern 'crash|NoSuchMethod|Exception' | Select-Object -First 5 | ForEach-Object { $_.Line }
        exit 1
    }
    if ($tail -match 'successfully loaded') {
        $ok = $true
        break
    }
}

if ($ok) {
    Write-Host 'GAME OK (7 mods, no crash)' -ForegroundColor Green
    exit 0
} else {
    Write-Host 'GAME NOT CONFIRMED (timeout, check logs\latest.log)' -ForegroundColor Yellow
    exit 1
}
