$InstallPath = "$env:C:\ProgramData\Chocolatey\bin"

$EnvPath = $env:PATH
if (!$EnvPath.ToLower().Contains($InstallPath.ToLower())) {

  Write-Host "Adding path to `'$InstallPath`'"

  $ActualPath = [Environment]::GetEnvironmentVariable('Path', [System.EnvironmentVariableTarget]::Machine)
  $Delimiter = ";"
  $HasDelimiter = $ActualPath -ne $null -and $ActualPath.EndsWith($Delimiter)
  If (!$HasDelimiter -and $ActualPath -ne $null) {$InstallPath = $Delimiter + $InstallPath}
  if (!$InstallPath.EndsWith($Delimiter)) {$InstallPath += $Delimiter}

  [Environment]::SetEnvironmentVariable('Path', $ActualPath + $InstallPath, [System.EnvironmentVariableTarget]::Machine)
}

$env:Path += ";$InstallPath"

if (!(Test-Path $InstallPath)) {
  Write-Host "Installing Chocolatey"
  iex ((new-object net.webclient).DownloadString('http://chocolatey.org/install.ps1'))
  Write-Host "Done!"
}
