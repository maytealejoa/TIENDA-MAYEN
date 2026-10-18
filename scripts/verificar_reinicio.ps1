param([switch]$EjecutarPostman, [switch]$VerificarCargaDatos)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$evidenceRoot = Join-Path $projectRoot 'target\apf2-verificacion'
New-Item -ItemType Directory -Path $evidenceRoot -Force | Out-Null
$jarPath = Join-Path $projectRoot 'target\gestion-0.0.1-SNAPSHOT.jar'
if (!(Test-Path -LiteralPath $jarPath)) { throw 'Primero ejecutar .\mvnw.cmd package' }
$javaPath = (Get-Command java.exe -ErrorAction Stop).Source
$listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
$listener.Start()
$demoPort = $listener.LocalEndpoint.Port
$listener.Stop()
$baseUrl = "http://127.0.0.1:$demoPort"
$runId = [Guid]::NewGuid().ToString('N')
$dbUrl = "jdbc:h2:file:./target/apf2-verificacion/datos-$runId;WRITE_DELAY=0"
$demoProcess = $null

function Start-Demo([int]$iteration) {
    $arguments = @('-jar','target/gestion-0.0.1-SNAPSHOT.jar',"--server.port=$demoPort",
        '--server.address=127.0.0.1','--spring.profiles.active=demo,consola',"--spring.datasource.url=$dbUrl")
    $script:demoProcess = Start-Process -FilePath $javaPath -ArgumentList $arguments -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $evidenceRoot "arranque-$iteration.log") `
        -RedirectStandardError (Join-Path $evidenceRoot "arranque-$iteration-error.log")
    for ($attempt = 0; $attempt -lt 45; $attempt++) {
        if ($script:demoProcess.HasExited) { throw "Falló el arranque; revisar arranque-$iteration.log" }
        try {
            $ping = Invoke-RestMethod -Uri "$baseUrl/api/public/ping" -TimeoutSec 2
            if ($ping.estado -eq 'OK') { return }
        } catch { Start-Sleep -Milliseconds 700 }
    }
    throw 'El servidor no respondió a tiempo'
}
function Stop-Demo {
    if ($null -ne $script:demoProcess -and !$script:demoProcess.HasExited) {
        # Solo detener el proceso creado por esta ejecución.
        Stop-Process -Id $script:demoProcess.Id
        $script:demoProcess.WaitForExit(10000) | Out-Null
    }
    $script:demoProcess = $null
}
function Login-Admin {
    # Tomcat puede responder ping antes de que ApplicationRunner termine el bootstrap.
    for ($attempt = 0; $attempt -lt 20; $attempt++) {
        try {
            $login = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/auth/login" -ContentType 'application/json' `
                -Body '{"username":"admin","password":"admin123"}'
            return @{Authorization="Bearer $($login.accessToken)"}
        } catch {
            if ($attempt -eq 19) {throw}
            Start-Sleep -Milliseconds 500
        }
    }
}

try {
    Start-Demo 1
    $authHeaders = Login-Admin
    $producto = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/productos" -Headers $authHeaders -ContentType 'application/json' `
        -Body '{"nombre":"Evidencia de reinicio APF2","precio":3.50,"stock":10,"stockMinimo":2,"fechaVencimiento":"2027-12-15"}'
    $ventaBody = @{items=@(@{productoId=$producto.id;cantidad=2})} | ConvertTo-Json -Depth 5
    $venta = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/ventas" -Headers $authHeaders -ContentType 'application/json' -Body $ventaBody
    # La consola usa Basic solo en su ruta local; la API sigue exigiendo JWT.
    $basicValue = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('admin:admin123'))
    $consoleResponse = Invoke-WebRequest -Uri "$baseUrl/h2-console/" -Headers @{Authorization="Basic $basicValue"} -UseBasicParsing
    if ($consoleResponse.StatusCode -ne 200) {throw 'Consola H2 no disponible para ADMIN'}
    $blockedStatus = 0
    try { $blockedStatus = (Invoke-WebRequest -Uri "$baseUrl/h2-console/" -UseBasicParsing).StatusCode }
    catch { if ($null -eq $_.Exception.Response) {throw}; $blockedStatus = [int]$_.Exception.Response.StatusCode }
    if ($blockedStatus -ne 401) {throw 'Consola H2 permitió acceso sin autenticación'}
    $browserSession = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
    $browserLogin = Invoke-WebRequest -Uri "$baseUrl/login" -WebSession $browserSession -UseBasicParsing
    if ($browserLogin.StatusCode -ne 200 -or $browserLogin.Content -notmatch 'name="username"') {throw 'Formulario de acceso H2 no disponible'}
    $browserConsole = Invoke-WebRequest -Method Post -Uri "$baseUrl/login" -Body @{username='admin';password='admin123'} `
        -ContentType 'application/x-www-form-urlencoded' -WebSession $browserSession -UseBasicParsing
    if ($browserConsole.StatusCode -ne 200 -or $browserConsole.Content -notmatch 'H2 Console|JDBC|login.jsp|login.do') {throw 'El formulario no permitió entrar a H2'}
    if ($EjecutarPostman) {
        Push-Location $projectRoot
        try {
            & npx.cmd --yes --package=newman newman run 'postman/Tienda_MAYEN_APF2.postman_collection.json' `
                --env-var "baseUrl=$baseUrl" --reporters cli,json --reporter-json-export 'target/apf2-verificacion/newman.json'
            if ($LASTEXITCODE -ne 0) {throw 'Falló la colección Postman'}
        } finally {Pop-Location}
    }
    if ($VerificarCargaDatos) {
        Push-Location $projectRoot
        try {
            for ($carga = 1; $carga -le 2; $carga++) {
                & npx.cmd --yes --package=newman newman run 'postman/Tienda_MAYEN_Datos.postman_collection.json' `
                    --env-var "baseUrl=$baseUrl" --reporters cli,json `
                    --reporter-json-export "target/apf2-verificacion/carga-$carga.json"
                if ($LASTEXITCODE -ne 0) {throw "Falló la colección de datos, ejecución $carga"}
                $catalogoActual = @(Invoke-RestMethod -Uri "$baseUrl/api/productos" -Headers (Login-Admin))
                if ($carga -eq 1) {$cantidadCatalogo = $catalogoActual.Count}
                elseif ($catalogoActual.Count -ne $cantidadCatalogo) {throw 'La segunda carga duplicó productos'}
            }
            'Catálogo reutilizado correctamente; cada ejecución registró cinco ventas nuevas.'
        } finally {Pop-Location}
    }
    Stop-Demo
    Start-Demo 2
    $authHeaders = Login-Admin
    $persistido = Invoke-RestMethod -Uri "$baseUrl/api/productos/$($producto.id)" -Headers $authHeaders
    $ventaPersistida = Invoke-RestMethod -Uri "$baseUrl/api/ventas/$($venta.id)" -Headers $authHeaders
    if ($persistido.stock -ne 8 -or $ventaPersistida.total -ne 7 -or $ventaPersistida.detalles.Count -ne 1) {
        throw 'Los datos no coinciden tras el reinicio'
    }
    [pscustomobject]@{Resultado='OK'; ProductoId=$producto.id; StockTrasReinicio=$persistido.stock;
        VentaId=$venta.id; TotalTrasReinicio=$ventaPersistida.total; ConsolaH2Admin=200; ConsolaSinCredenciales=401;
        FormularioH2=200; LoginFormularioH2=200; PostmanEjecutado=[bool]$EjecutarPostman;
        CargaDatosVerificada=[bool]$VerificarCargaDatos; BaseDeDatos=$dbUrl} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $evidenceRoot 'reinicio.json') -Encoding utf8
    'Verificación OK: producto, venta y stock sobreviven al reinicio. Evidencia en target/apf2-verificacion/reinicio.json'
} finally { Stop-Demo }
