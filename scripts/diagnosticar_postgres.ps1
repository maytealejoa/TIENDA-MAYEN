$ErrorActionPreference = 'Stop'
$taskProject = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$taskOutput = Join-Path $taskProject 'target\diagnostico-postgres'
New-Item -ItemType Directory -Path $taskOutput -Force | Out-Null
$taskOriginal = Get-CimInstance Win32_Process -Filter "name='javaw.exe'" | Where-Object { $_.CommandLine -like '*com.tienda.gestion.GestionApplication*' } | Select-Object -First 1
if (!$taskOriginal) { throw 'No se encontró la ejecución de gestion para reutilizar su classpath.' }
$taskClasspath = [regex]::Match($taskOriginal.CommandLine, '-classpath "([^"]+)"').Groups[1].Value
if (!$taskClasspath) { throw 'No se pudo leer el classpath de Eclipse.' }
$taskClasspath = (Join-Path $taskProject 'src\main\resources') + ';' + $taskClasspath
$taskJava = Join-Path (Split-Path $taskOriginal.ExecutablePath) 'java.exe'
$taskListener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,0)
$taskListener.Start(); $taskPort = $taskListener.LocalEndpoint.Port; $taskListener.Stop()
$taskArgs = @('-classpath', ('"' + $taskClasspath + '"'), '-Dspring.devtools.restart.enabled=false', 'com.tienda.gestion.GestionApplication', '--spring.profiles.active=demo,postgres', "--server.port=$taskPort", '--spring.jpa.hibernate.ddl-auto=none', '--app.bootstrap.enabled=false', '--spring.jpa.show-sql=false')
$taskProcess = Start-Process -FilePath $taskJava -ArgumentList $taskArgs -WorkingDirectory $taskProject -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $taskOutput 'salida.log') -RedirectStandardError (Join-Path $taskOutput 'error.log')
try {
    $taskReady = $false
    for ($taskAttempt=0; $taskAttempt -lt 45; $taskAttempt++) {
        if ($taskProcess.HasExited) { break }
        try {
            $taskPing = Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:$taskPort/api/public/ping" -TimeoutSec 2
            if ($taskPing.StatusCode -eq 200) { $taskReady = $true; break }
        } catch {}
        Start-Sleep -Seconds 1
    }
    $taskLog = Get-Content -Raw -LiteralPath (Join-Path $taskOutput 'salida.log')
    $taskPostgres = $taskLog -match 'PostgreSQL' -and $taskLog -match 'jdbc:postgresql:'
    [pscustomobject]@{ArranqueCorrecto=$taskReady;ConexionPostgreSQL=$taskPostgres;PuertoPrueba=$taskPort;CreacionTablasDeshabilitada=$true;InicializacionUsuariosDeshabilitada=$true} | ConvertTo-Json | Tee-Object -FilePath (Join-Path $taskOutput 'resultado.json')
    if (!$taskReady -or !$taskPostgres) {
        Get-Content -LiteralPath (Join-Path $taskOutput 'salida.log') -Tail 35
        throw 'Revisar los registros de diagnóstico.'
    }
} finally {
    if (!$taskProcess.HasExited) { Stop-Process -Id $taskProcess.Id }
}
