$archivo = (Resolve-Path ".\README.md").Path

$contenido = [System.IO.File]::ReadAllText(
    $archivo,
    [System.Text.Encoding]::UTF8
)

$contenido = [regex]::Replace(
    $contenido,
    '[ \t]+(?=\r?$)',
    '',
    [System.Text.RegularExpressions.RegexOptions]::Multiline
)

$contenido = $contenido.TrimEnd() + "`n"

[System.IO.File]::WriteAllText(
    $archivo,
    $contenido,
    (New-Object System.Text.UTF8Encoding($false))
)