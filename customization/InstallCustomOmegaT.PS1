# Version 1.2
# Authors: Valetina Nardo, @msoutopico

# run as
# Invoke-Expression ((New-Object System.Net.WebClient).DownloadString('https://gist.githubusercontent.com/msoutopico/b6964805cfab8d50ef24d050b441b055/raw/f0983c4ab64b6ba2bc93c968b0b682e9b0fba590/InstallCustomOmegaT.PS1'))
# or
# Invoke-Expression ((New-Object System.Net.WebClient).DownloadString('https://cat.capstan.be/OmegaT/InstallCustomOmegaT.PS1'))

# Tested in:
# Windows 10: pass
# Windows 7: fail


#0 Give instructions on how to stop the script
Clear
Write-Host "Press ctrl+C at any time to stop the script"


#1. The script finds out whether the machine is 64bits or 32bits
#2. The script downloads the appropriate OmegaT installer for the kind of machine

if ((Get-WmiObject win32_operatingsystem | select osarchitecture).osarchitecture -like "64*")
{
#64bit download url
$url = "https://sourceforge.net/projects/omegat/files/OmegaT%20-%20Latest/OmegaT%204.2.0/OmegaT_4.2.0_Beta_Windows_64_Signed.exe/download"
}
else
{
#32bit download url
$url = "https://sourceforge.net/projects/omegat/files/OmegaT%20-%20Latest/OmegaT%204.2.0/OmegaT_4.2.0_Beta_Windows_Signed.exe/download"
}

$targetFile = "OmegaT_4.2.0_Signed.exe"



#3a. Funtion for downloading with progress bar

function DownloadFile($url, $targetFile)
{
   $uri = New-Object "System.Uri" "$url"
   $request = [System.Net.HttpWebRequest]::Create($uri)
   $request.set_Timeout(15000) #15 second timeout
   $response = $request.GetResponse()
   #$totalLength = [System.Math]::Floor($response.get_ContentLength()/1024)
   $responseContentLength = $response.get_ContentLength()

   if (-not ($responseContentLength -lt 1024))
   {
      $totalLength = [System.Math]::Floor($response.get_ContentLength()/1024)
   }
   else
   {
      $totalLength = [System.Math]::Floor(1024/1024)
   }

   $responseStream = $response.GetResponseStream()
   $targetStream = New-Object -TypeName System.IO.FileStream -ArgumentList $targetFile, Create
   $buffer = new-object byte[] 400KB
   $count = $responseStream.Read($buffer,0,$buffer.length)
   $downloadedBytes = $count

   while ($count -gt 0)
   {
       $targetStream.Write($buffer, 0, $count)
       $count = $responseStream.Read($buffer,0,$buffer.length)
       $downloadedBytes = $downloadedBytes + $count
       Write-Progress -activity "Downloading '$targetFile'" -status "Downloaded ($([System.Math]::Floor($downloadedBytes/1024))K of $($totalLength)K): " -PercentComplete ((([System.Math]::Floor($downloadedBytes/1024)) / $totalLength)  * 100)
   }

   Write-Progress -activity "Finished downloading file"
   $targetStream.Flush()
   $targetStream.Close()
   $targetStream.Dispose()
   $responseStream.Dispose()
}


#3b. Download OmegaT
DownloadFile $url $targetFile


#3c. The script downloads the necessary custom files

$urlConfig = "https://cat.capstan.be/OmegaT/manual/config.zip"
$urlPlugins = "https://cat.capstan.be/OmegaT/manual/plugins.zip"
$urlScripts = "https://cat.capstan.be/OmegaT/manual/scripts.zip"

$outConfig = "config.zip"
$outPlugins = "plugins.zip"
$outScripts = "scripts.zip"

$url = $urlConfig
$targetFile = $outConfig
DownloadFile $url $targetFile

$url = $urlPlugins
$targetFile = $outPlugins
DownloadFile $url $targetFile

$url = $urlScripts
$targetFile = $outScripts
DownloadFile $url $targetFile


#4. The script executes the OmegaT installer with default parameters

$DesktopPath = [Environment]::GetFolderPath("Desktop")
#Start-Process -FilePath $DesktopPath\OmegaT_4.2.0_Signed.exe -Args '/silent /install' -Verb RunAs -Wait;


#5. The script unzips and writes all custom files in the appropriate location (%APPDATA%/OmegaT)

Expand-Archive -LiteralPath $DesktopPath\config.zip -DestinationPath $env:APPDATA\OmegaT\ -Force
Expand-Archive -LiteralPath $DesktopPath\plugins.zip -DestinationPath $env:APPDATA\OmegaT\plugins -Force
Expand-Archive -LiteralPath $DesktopPath\scripts.zip -DestinationPath $env:APPDATA\OmegaT\scripts -Force


#6 Delete newly downloaded OmegaT installer and zip folders after installation/extraction

if (Test-Path -Path $DesktopPath\OmegaT_4.2.0_Signed.exe) {
  Remove-Item -Path $DesktopPath\OmegaT_4.2.0_Signed.exe
}
if (Test-Path -Path $DesktopPath\config.zip) {
  Remove-Item -Path $DesktopPath\config.zip
}
if (Test-Path -Path $DesktopPath\plugins.zip) {
  Remove-Item -Path $DesktopPath\plugins.zip
}
if (Test-Path -Path $DesktopPath\scripts.zip) {
  Remove-Item -Path $DesktopPath\scripts.zip
}

#7 If <scripts_dir> exists, update the indicated path, otherwise add info within the preference tag.

$newText = Join-Path $env:APPDATA\ '\OmegaT\scripts'
$getScript = Get-Content $env:APPDATA\OmegaT\omegat.prefs

$open = "<scripts_dir>"
$close = "</scripts_dir>"
$pattern = "$open(.*?)$close"

$result = [regex]::Match($getScript,$pattern).Groups[1].Value


If (-Not $result -eq "") {
		$getScript -replace [regex]::Escape($result), $newText |
		Set-Content $env:APPDATA\OmegaT\omegat.prefs

} else {
	$newText = "<scripts_dir>" + $newText + "</scripts_dir>`n</preference>"
	$getScript | ForEach-Object {$_ -Replace '</preference>', ($newText)} |
		Set-Content $env:APPDATA\OmegaT\omegat.prefs
}



#8 Clear console, display completion msg and close window after 7 seconds
Clear
Write-Host "`n`n`n`nOmegaT was installed with its configuration, plugins, and scripts files."
Start-Sleep 7
