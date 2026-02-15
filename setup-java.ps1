# Script to set JAVA_HOME permanently

$javaHome = "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"

Write-Host "Setting JAVA_HOME to: $javaHome" -ForegroundColor Green

# Set for current session
$env:JAVA_HOME = $javaHome
$env:PATH = "$javaHome\bin;$env:PATH"

# Set permanently (requires admin, will ask for permission)
try {
    [Environment]::SetEnvironmentVariable("JAVA_HOME", $javaHome, "Machine")
    $currentPath = [Environment]::GetEnvironmentVariable("Path", "Machine")
    if ($currentPath -notlike "*$javaHome\bin*") {
        [Environment]::SetEnvironmentVariable("Path", "$javaHome\bin;$currentPath", "Machine")
    }
    Write-Host "✅ JAVA_HOME set permanently!" -ForegroundColor Green
    Write-Host "✅ Added to PATH!" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Could not set permanently (need admin). Set for current session only." -ForegroundColor Yellow
}

# Verify
Write-Host "`nVerifying installation:" -ForegroundColor Cyan
& java -version

Write-Host "`nJAVA_HOME is: $env:JAVA_HOME" -ForegroundColor Cyan
Write-Host "`nYou can now run: .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev" -ForegroundColor Green
