param(
    [Alias('v')]
    [switch]$RemoveVolumes
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

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

    throw 'Docker Compose is not installed.'
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

Initialize-Compose

if ($RemoveVolumes) {
    Invoke-Compose down -v
} else {
    Invoke-Compose down
}
