$envFile = Join-Path $PSScriptRoot '.env'

if (-not (Test-Path -LiteralPath $envFile)) {
    throw 'Missing BE/.env. Create it with GEMINI_API_KEY=your-key.'
}

Get-Content -LiteralPath $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#')) {
        $name, $value = $line -split '=', 2
        if ($name -and $value) {
            Set-Item -Path "Env:$($name.Trim())" -Value $value.Trim()
        }
    }
}

if (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'mvnw.cmd')) {
    & (Join-Path $PSScriptRoot 'mvnw.cmd') spring-boot:run
} else {
    mvn spring-boot:run
}
