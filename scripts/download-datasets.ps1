param(
    [string]$OutputDir = "docs/datasets"
)

$ErrorActionPreference = "Stop"

$datasets = @(
    @{
        Name         = "Volcanes.geojson"
        Service      = "https://services1.arcgis.com/Og2nrTKe5bptW02d/arcgis/rest/services/MAPAGEOLOGIA/FeatureServer"
        Layer        = 0
        Expected     = 61
        PageSize     = 1000
        Source       = "SGC - Mapa Geologico de Colombia 2015, capa Volcanes"
    },
    @{
        Name         = "Fallas.geojson"
        Service      = "https://services1.arcgis.com/Og2nrTKe5bptW02d/arcgis/rest/services/MAPAGEOLOGIA/FeatureServer"
        Layer        = 1
        Expected     = 4866
        PageSize     = 1000
        Source       = "SGC - Mapa Geologico de Colombia 2015, capa Fallas"
    },
    @{
        Name         = "Mapa_Geologico_de_Colombia_2015.geojson"
        Service      = "https://services1.arcgis.com/Og2nrTKe5bptW02d/arcgis/rest/services/MAPAGEOLOGIA/FeatureServer"
        Layer        = 4
        Expected     = 7461
        PageSize     = 1000
        Source       = "SGC - Mapa Geologico de Colombia 2015, unidades cronoestratigraficas"
    },
    @{
        Name         = "Mapa_Tectonico_de_Colombia_2017.geojson"
        Service      = "https://services1.arcgis.com/Og2nrTKe5bptW02d/arcgis/rest/services/Mapa_Tect%C3%B3nico_de_Colombia_2017_Dominios_Tect%C3%B3nicosDominios_Tect%C3%B3nicos/FeatureServer"
        Layer        = 0
        Expected     = 3
        PageSize     = 2000
        Source       = "SGC - Mapa Tectonico de Colombia 2017, dominios tectonicos"
    },
    @{
        Name         = "Inventario_de_movimientos_en_masa.geojson"
        Service      = "https://services1.arcgis.com/Og2nrTKe5bptW02d/arcgis/rest/services/Inventario_de_movimientos_en_masa/FeatureServer"
        Layer        = 0
        Expected     = 6826
        PageSize     = 2000
        Source       = "SGC - Inventario de movimientos en masa"
    }
)

function Get-FeatureCount {
    param([string]$Service, [int]$Layer)
    $url = "$Service/$Layer/query?f=json&where=1%3D1&returnCountOnly=true"
    $response = Invoke-RestMethod -Uri $url -TimeoutSec 120
    return [long]$response.count
}

function Get-FeaturePage {
    param([string]$Service, [int]$Layer, [int]$Offset, [int]$Limit)
    $url = "$Service/$Layer/query?f=geojson&where=1%3D1&outFields=*&outSR=4326&resultOffset=$Offset&resultRecordCount=$Limit"
    return Invoke-RestMethod -Uri $url -TimeoutSec 600
}

function Write-FeatureCollection {
    param([System.Collections.ArrayList]$Features, [string]$Dest)
    $collection = [ordered]@{
        type     = "FeatureCollection"
        features = $Features
    }
    $json = $collection | ConvertTo-Json -Depth 100
    [System.IO.File]::WriteAllText($Dest, $json, [System.Text.UTF8Encoding]::new($false))
}

if (-not (Test-Path -LiteralPath $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

$allOk = $true
foreach ($dataset in $datasets) {
    $dest = Join-Path $OutputDir $dataset.Name
    Write-Host "== $($dataset.Name)"
    Write-Host "   Source: $($dataset.Source)"

    if (Test-Path -LiteralPath $dest) {
        try {
            $existing = Get-Content -LiteralPath $dest -Raw | ConvertFrom-Json
            if ($existing.type -eq "FeatureCollection" -and $existing.features.Count -eq $dataset.Expected) {
                Write-Host "   Already present and complete ($($existing.features.Count) features); skipping."
                continue
            }
            Write-Host "   Existing file is incomplete or invalid; re-downloading."
        }
        catch {
            Write-Host "   Existing file is not valid JSON; re-downloading."
        }
    }

    try {
        $total = Get-FeatureCount -Service $dataset.Service -Layer $dataset.Layer
        Write-Host "   Source reports $total features."

        $features = New-Object System.Collections.ArrayList
        $offset = 0
        while ($offset -lt $total) {
            $page = Get-FeaturePage -Service $dataset.Service -Layer $dataset.Layer -Offset $offset -Limit $dataset.PageSize
            foreach ($feature in $page.features) {
                [void]$features.Add($feature)
            }
            $offset += $dataset.PageSize
        }

        if ($features.Count -ne $dataset.Expected) {
            Write-Host "   WARNING: downloaded $($features.Count) features, expected $($dataset.Expected)."
            $allOk = $false
            continue
        }

        Write-FeatureCollection -Features $features -Dest $dest
        Write-Host "   Downloaded and verified: $($features.Count) features."
    }
    catch {
        Write-Host "   FAILED: $($_.Exception.Message)"
        $allOk = $false
    }
}

if ($allOk) {
    Write-Host ""
    Write-Host "All datasets are ready in $OutputDir."
}
else {
    Write-Output ""
    Write-Output "Some datasets could not be downloaded or verified. Run the script again to retry."
}
