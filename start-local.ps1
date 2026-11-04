# ============================================================
# start-local.ps1 - Khoi dong du an Oboe chay LOCAL (khong can AWS)
# Mo 3 cua so: MySQL (3306) + Backend (8080) + Frontend (5175)
#
# Cach chay:
#   powershell -ExecutionPolicy Bypass -File .\start-local.ps1
# ============================================================

$root     = $PSScriptRoot
$mysqlDir = Join-Path $root '.tools\mysql\mysql-8.0.40-winx64'
$jdkDir   = Join-Path $root '.tools\jdk21\jdk-21.0.13+9'
$backend  = Join-Path $root 'backend\Oboe'
$frontend = Join-Path $root 'frontend'
$backendEnvLocal = Join-Path $backend '.env.local'

# ---------- Kiem tra cong cu portable ----------
if (-not (Test-Path (Join-Path $mysqlDir 'bin\mysqld.exe'))) {
    Write-Host "!! Khong tim thay MySQL tai: $mysqlDir" -ForegroundColor Red
    exit 1
}
if (-not (Test-Path (Join-Path $jdkDir 'bin\java.exe'))) {
    Write-Host "!! Khong tim thay JDK 21 tai: $jdkDir" -ForegroundColor Red
    exit 1
}

function Test-PortListening([int]$port) {
    try { return [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction Stop) }
    catch { return $false }
}

function Start-Service-Window([string]$title, [string]$command, [string]$workDir) {
    Start-Process powershell -ArgumentList "-NoExit -Command `"`$host.UI.RawUI.WindowTitle='$title'; $command`"" -WorkingDirectory $workDir
}

if (-not (Test-Path $backendEnvLocal)) {
    Write-Host "[AI]       chua co file backend\\Oboe\\.env.local (AI Sensei se chua dung duoc)." -ForegroundColor Yellow
} elseif (-not (Select-String -Path $backendEnvLocal -Pattern '^GEMINI_API_KEY\s*=\s*\S+' -Quiet)) {
    Write-Host "[AI]       GEMINI_API_KEY dang de trong trong backend\\Oboe\\.env.local." -ForegroundColor Yellow
}

# ---------- 1. MySQL ----------
if (Test-PortListening 3306) {
    Write-Host '[MySQL]    da chay san (port 3306) - bo qua.' -ForegroundColor Yellow
} else {
    Write-Host '[MySQL]    dang khoi dong...' -ForegroundColor Cyan
    $mysqlCmd = "& '$mysqlDir\bin\mysqld.exe' --datadir='$mysqlDir\data' --port=3306 --console"
    Start-Service-Window 'OBOE-MySQL' $mysqlCmd $mysqlDir

    Write-Host '[MySQL]    cho san sang...' -NoNewline
    $ready = $false
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 1
        if (Test-PortListening 3306) { $ready = $true; break }
        Write-Host '.' -NoNewline
    }
    Write-Host ''
    if ($ready) { Write-Host '[MySQL]    OK.' -ForegroundColor Green }
    else { Write-Host '[MySQL]    chua san sang sau 30s, van tiep tuc...' -ForegroundColor Yellow }
}

# ---------- 2. Backend (Spring Boot) ----------
if (Test-PortListening 8080) {
    Write-Host '[Backend]  da chay san (port 8080) - bo qua.' -ForegroundColor Yellow
} else {
    Write-Host '[Backend]  dang khoi dong (lan dau tai thu vien co the lau)...' -ForegroundColor Cyan
    $backendCmd = "`$env:JAVA_HOME='$jdkDir'; & '.\mvnw.cmd' spring-boot:run '-Dspring-boot.run.profiles=local'"
    Start-Service-Window 'OBOE-Backend' $backendCmd $backend
}

# ---------- 3. Frontend (Vite) ----------
if (Test-PortListening 5175) {
    Write-Host '[Frontend] da chay san (port 5175) - bo qua.' -ForegroundColor Yellow
} else {
    Write-Host '[Frontend] dang khoi dong...' -ForegroundColor Cyan
    if (-not (Test-Path (Join-Path $frontend 'node_modules'))) {
        Write-Host '[Frontend] chua co node_modules - dang chay npm install...' -ForegroundColor Cyan
        Push-Location $frontend
        npm install
        Pop-Location
    }
    Start-Service-Window 'OBOE-Frontend' 'npm run dev' $frontend
}

Write-Host ''
Write-Host '============================================' -ForegroundColor Green
Write-Host '  DU AN DANG KHOI DONG' -ForegroundColor Green
Write-Host '  Frontend : http://localhost:5175' -ForegroundColor Green
Write-Host '  Backend  : http://localhost:8080' -ForegroundColor Green
Write-Host '  MySQL    : localhost:3306  (db: oboe_db, user: root)' -ForegroundColor Green
Write-Host '============================================' -ForegroundColor Green
Write-Host 'Dang nhap thu: 0900000000 / 12345678'
Write-Host 'Tat: dong tung cua so tuong ung (hoac Ctrl+C trong cua so do).'
