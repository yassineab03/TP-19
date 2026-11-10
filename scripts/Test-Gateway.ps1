param(
    [Parameter(Mandatory = $true)][string]$JavaPath,
    [switch]$IncludeVoiture
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$processes = [System.Collections.Generic.List[System.Diagnostics.Process]]::new()

function Start-LabService([string]$Module, [string]$Label, [string[]]$Options) {
    $jar = Join-Path $projectRoot "$Module/target/$Module-1.0.0-SNAPSHOT.jar"
    $process = Start-Process -FilePath $JavaPath -ArgumentList (@('-Xmx256m', '-jar', "`"$jar`"") + $Options) `
        -WindowStyle Hidden -PassThru -WorkingDirectory $projectRoot `
        -RedirectStandardOutput (Join-Path $projectRoot "$Label.out.log") `
        -RedirectStandardError (Join-Path $projectRoot "$Label.err.log")
    $processes.Add($process)
}

function Wait-Json([string]$Url, [scriptblock]$Accept) {
    $deadline = [DateTime]::UtcNow.AddSeconds(90)
    do {
        try {
            $result = Invoke-RestMethod -Uri $Url -TimeoutSec 3
            if (& $Accept $result) { return }
        } catch { }
        Start-Sleep -Seconds 2
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "Échec de vérification : $Url. Consulter les fichiers step6-*.log."
}

# Ports isolés : ne touche pas aux applications lancées dans IntelliJ.
foreach ($port in @(18761, 18088, 18888, 18889)) {
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) {
        throw "Le port de test $port est déjà utilisé."
    }
}
if ($IncludeVoiture -and (Get-NetTCPConnection -LocalPort 18089 -State Listen -ErrorAction SilentlyContinue)) {
    throw 'Le port de test 18089 est déjà utilisé.'
}

try {
    Start-LabService 'eureka-server' 'step6-eureka' @('--server.port=18761')
    Wait-Json 'http://localhost:18761/actuator/health' { param($r) $r.status -eq 'UP' }

    $discoveryOptions = @(
        '--eureka.client.service-url.defaultZone=http://localhost:18761/eureka/',
        '--eureka.instance.ip-address=127.0.0.1',
        '--eureka.instance.lease-renewal-interval-in-seconds=1',
        '--eureka.client.initial-instance-info-replication-interval-seconds=1',
        '--eureka.client.registry-fetch-interval-seconds=1'
    )
    Start-LabService 'service-client' 'step6-client' (@('--server.port=18088') + $discoveryOptions)
    Wait-Json 'http://localhost:18088/clients' { param($r) @($r).Count -eq 3 }
    Start-LabService 'gateway' 'step6-gateway' (@('--server.port=18888') + $discoveryOptions)

    foreach ($path in @('/clients', '/SERVICE-CLIENT/clients')) {
        Wait-Json "http://localhost:18888$path" {
            param($r) @($r).Count -eq 3 -and $r[0].nom -eq 'Rabab SELIMANI'
        }
        Write-Output "OK dynamique : $path"
    }
    foreach ($path in @('/client/1', '/clients/1', '/SERVICE-CLIENT/client/1')) {
        Wait-Json "http://localhost:18888$path" { param($r) $r.id -eq 1 -and $r.nom -eq 'Rabab SELIMANI' }
        Write-Output "OK dynamique : $path"
    }

    $registry = Invoke-RestMethod -Uri 'http://localhost:18761/eureka/apps' -Headers @{ Accept = 'application/json' }
    $registeredNames = @($registry.applications.application | ForEach-Object { $_.name })
    foreach ($name in @('SERVICE-CLIENT', 'GATEWAY')) {
        if ($registeredNames -notcontains $name) { throw "$name absent du registre Eureka" }
    }
    Write-Output 'OK : Client et Gateway enregistrés dans Eureka'

    if ($IncludeVoiture) {
        Start-LabService 'service-voiture' 'step7-voiture' (@('--server.port=18089') + $discoveryOptions)
        foreach ($url in @('http://localhost:18089/voitures', 'http://localhost:18888/SERVICE-VOITURE/voitures')) {
            Wait-Json $url {
                param($r)
                @($r).Count -eq 3 -and
                $r[0].marque -eq 'Toyota' -and $r[0].matricule -eq 'A 25 333' -and
                $r[0].client.id -eq 1 -and $r[0].client.nom -eq 'Rabab SELIMANI' -and
                $r[1].client.id -eq 1 -and $r[2].client.id -eq 2 -and $r[2].client.nom -eq 'Amal RAMI'
            }
            Write-Output "OK Voiture + Feign : $url"
        }
        Wait-Json 'http://localhost:18089/voitures/client/1' { param($r) @($r).Count -eq 2 }
        Wait-Json 'http://localhost:18089/voiture/3' { param($r) $r.model -eq '301' -and $r.client.id -eq 2 }
        $missing = Invoke-WebRequest -Uri 'http://localhost:18089/voitures/999' -SkipHttpErrorCheck
        if ($missing.StatusCode -ne 404) { throw 'Une voiture absente doit retourner 404' }
        $null = Invoke-RestMethod -Method Put -Uri 'http://localhost:18088/clients/1' `
            -ContentType 'application/json' -Body '{"nom":"Client modifie","age":24}'
        Wait-Json 'http://localhost:18089/voiture/1' { param($r) $r.client.nom -eq 'Client modifie' }
        Write-Output 'OK : Feign relit les donnees Client apres modification'
        $registry = Invoke-RestMethod -Uri 'http://localhost:18761/eureka/apps' -Headers @{ Accept = 'application/json' }
        if (@($registry.applications.application.name) -notcontains 'SERVICE-VOITURE') {
            throw 'SERVICE-VOITURE absent du registre Eureka'
        }
        Write-Output 'OK : SERVICE-VOITURE enregistre dans Eureka'
    }

    Start-LabService 'gateway' 'step6-static' @(
        '--server.port=18889', '--spring.profiles.active=static',
        '--spring.cloud.gateway.server.webflux.routes[0].uri=http://localhost:18088',
        '--spring.cloud.gateway.server.webflux.routes[0].id=clients-static',
        '--spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/clients,/clients/**',
        '--spring.cloud.gateway.server.webflux.routes[1].uri=http://localhost:18088',
        '--spring.cloud.gateway.server.webflux.routes[1].id=client-static',
        '--spring.cloud.gateway.server.webflux.routes[1].predicates[0]=Path=/client/**'
    )
    Wait-Json 'http://localhost:18889/clients' { param($r) @($r).Count -eq 3 }
    Wait-Json 'http://localhost:18889/client/1' { param($r) $r.id -eq 1 }
    Write-Output "OK : profil statique de l'étape 5 conservé"
} finally {
    foreach ($process in $processes) {
        if (-not $process.HasExited) { Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue }
    }
}
