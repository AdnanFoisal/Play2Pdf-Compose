# Generates app launcher icons from mock assests/app icon.jpeg
# - Legacy square + round PNG mipmaps (pre-API-26)
# - Full-bleed adaptive-icon background bitmaps (API 26+)
Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$src  = Join-Path $root 'mock assests/app icon.jpeg'
$resDir = Join-Path $root 'app/src/main/res'

$source = [System.Drawing.Image]::FromFile($src)
Write-Host "Loaded source: $($source.Width)x$($source.Height)"

# Densities and their base launcher sizes (dp = 48 baseline)
$densities = @{
    'mdpi'    = 48
    'hdpi'    = 72
    'xhdpi'   = 96
    'xxhdpi'  = 144
    'xxxhdpi' = 192
}

function Save-Square($size, $path, [bool]$round) {
    $bmp = New-Object System.Drawing.Bitmap $size, $size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    if ($round) {
        $gp = New-Object System.Drawing.Drawing2D.GraphicsPath
        $gp.AddEllipse(0, 0, $size, $size)
        $g.SetClip($gp)
    }
    # Cover-fit: scale source so the square is fully covered, center-crop
    $s = [Math]::Max($size / $source.Width, $size / $source.Height)
    $w = $source.Width * $s
    $h = $source.Height * $s
    $x = ($size - $w) / 2
    $y = ($size - $h) / 2
    $g.DrawImage($source, $x, $y, $w, $h)
    $g.Dispose()
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "  wrote $path"
}

foreach ($d in $densities.Keys) {
    $base = $densities[$d]
    $dir = Join-Path $resDir "mipmap-$d"
    New-Item -ItemType Directory -Force -Path $dir | Out-Null

    # Legacy square + round launcher icons
    Save-Square $base (Join-Path $dir 'ic_launcher.png') $false
    Save-Square $base (Join-Path $dir 'ic_launcher_round.png') $true

    # Adaptive-icon full-bleed background: 108dp canvas => base * 108/48 = base * 2.25
    $adaptive = [int]($base * 2.25)
    Save-Square $adaptive (Join-Path $dir 'ic_launcher_bg.png') $false
}

$source.Dispose()
Write-Host "DONE"

