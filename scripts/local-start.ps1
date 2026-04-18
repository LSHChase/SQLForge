Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Split-Path -Parent $ScriptDir
$script:ComposeCommand = $null

function Initialize-Compose {
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        try {
            & docker compose version *> $null
            $script:ComposeCommand = @('docker', 'compose')
            return
        } catch {
        }
    }

    if (Get-Command docker-compose -ErrorAction SilentlyContinue) {
        $script:ComposeCommand = @('docker-compose')
        return
    }

    throw 'Docker Compose is not installed. Install Docker Desktop first.'
}

function Invoke-Compose {
    param(
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$Args
    )

    if ($script:ComposeCommand.Count -eq 2) {
        & $script:ComposeCommand[0] $script:ComposeCommand[1] @Args
        return
    }

    & $script:ComposeCommand[0] @Args
}

function Test-LocalPortInUse {
    param([int]$Port)

    try {
        $listeners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop
        return $listeners.Count -gt 0
    } catch {
        return $false
    }
}

function Test-TcpPort {
    param(
        [string]$Host,
        [int]$Port
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $asyncResult = $client.BeginConnect($Host, $Port, $null, $null)
        $connected = $asyncResult.AsyncWaitHandle.WaitOne(1000, $false)
        if (-not $connected) {
            return $false
        }
        $client.EndConnect($asyncResult)
        return $true
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Wait-ForPort {
    param(
        [string]$Host,
        [int]$Port,
        [int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-TcpPort -Host $Host -Port $Port) {
            return
        }
        Start-Sleep -Seconds 2
    }

    throw "Timed out waiting for ${Host}:${Port} after ${TimeoutSeconds}s."
}

function Wait-ForMySqlReady {
    $deadline = (Get-Date).AddSeconds(60)
    while ((Get-Date) -lt $deadline) {
        try {
            & docker exec sqlforge-mysql mysqladmin ping -h 127.0.0.1 -uroot -psqlforge --silent *> $null
            return
        } catch {
            Start-Sleep -Seconds 2
        }
    }

    throw 'MySQL port is reachable, but the container did not become ready within 60 seconds.'
}

function Invoke-MySqlScript {
    param([string]$RelativePath)

    Write-Host "Applying $RelativePath..."
    Get-Content -Raw (Join-Path $RepoRoot $RelativePath) | & docker exec -i sqlforge-mysql mysql -uroot -psqlforge sqlforge
}

Set-Location $RepoRoot

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker is not installed. Install Docker Desktop first.'
}

Initialize-Compose

foreach ($port in 3306, 6379, 9092, 9000) {
    if (Test-LocalPortInUse -Port $port) {
        throw "Port $port is already in use. Release it before starting the local environment."
    }
}

Write-Host 'Starting local infrastructure with Docker Compose...'
Invoke-Compose up -d

Write-Host 'Waiting for MySQL port 3306...'
Wait-ForPort -Host '127.0.0.1' -Port 3306 -TimeoutSeconds 60
Wait-ForMySqlReady

Invoke-MySqlScript -RelativePath 'sql/init-schema.sql'
Invoke-MySqlScript -RelativePath 'sql/init-data.sql'

Write-Host 'Local services are ready:'
Write-Host '- MySQL: mysql://root:sqlforge@localhost:3306/sqlforge'
Write-Host '- Redis: redis://localhost:6379'
Write-Host '- Kafka: localhost:9092'
Write-Host '- MinIO API: http://localhost:9000'
Write-Host '- MinIO Console: http://localhost:9001'
