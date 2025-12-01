# SolaceQueueBrowserGui Password Encryption Script (PowerShell)
# Wrapper script for encrypting/decrypting passwords using PasswordEncryptionCLI

param(
    [Parameter(Position=0)]
    [ValidateSet("encrypt", "decrypt")]
    [string]$Command = "encrypt",
    
    [Parameter(Position=1)]
    [string]$Password = "",
    
    [Parameter(Position=2)]
    [string]$MasterKey = "",
    
    [Parameter()]
    [Alias("h")]
    [switch]$Help
)

# Change to the project directory (parent of scripts/)
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location (Join-Path $ScriptDir "..")

# Show help if requested
if ($Help) {
    Write-Host ""
    Write-Host "Usage: $($MyInvocation.MyCommand.Name) [command] [options]"
    Write-Host ""
    Write-Host "Commands:"
    Write-Host "  encrypt [password] [master-key]  - Encrypt a password"
    Write-Host "  decrypt [encrypted] [master-key]  - Decrypt a password"
    Write-Host "  -h, -Help                        - Show this help message"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  # Encrypt (interactive mode - recommended, passwords hidden):"
    Write-Host "  .\crypt-util.ps1 encrypt"
    Write-Host ""
    Write-Host "  # Encrypt (non-interactive mode - for scripts):"
    Write-Host "  .\crypt-util.ps1 encrypt 'myPassword' 'masterKey'"
    Write-Host ""
    Write-Host "  # Decrypt (interactive):"
    Write-Host "  .\crypt-util.ps1 decrypt"
    Write-Host ""
    Write-Host "  # Decrypt (non-interactive):"
    Write-Host "  .\crypt-util.ps1 decrypt 'ENC:AES256GCM:...' 'masterKey'"
    Write-Host ""
    Write-Host "Note: Always quote passwords with special characters!"
    exit 0
}

# Find the JAR file - check both development and distribution locations
$JarFile = $null

# First, check in target/ directory (development environment)
if (Test-Path "target") {
    $jarFiles = Get-ChildItem -Path "target" -Filter "SolaceQueueBrowserGui-*-jar-with-dependencies.jar" -ErrorAction SilentlyContinue
    if ($jarFiles) {
        $JarFile = $jarFiles[0].FullName
    }
}

# If not found, check in current directory (distribution package)
if (-not $JarFile) {
    $jarFiles = Get-ChildItem -Path "." -Filter "SolaceQueueBrowserGui-*-jar-with-dependencies.jar" -ErrorAction SilentlyContinue
    if ($jarFiles) {
        $JarFile = $jarFiles[0].FullName
    }
}

# If still not found, check in parent directory (in case we're in a subdirectory)
if (-not $JarFile) {
    $jarFiles = Get-ChildItem -Path ".." -Filter "SolaceQueueBrowserGui-*-jar-with-dependencies.jar" -ErrorAction SilentlyContinue
    if ($jarFiles) {
        $JarFile = $jarFiles[0].FullName
    }
}

if (-not $JarFile) {
    Write-Host "Error: JAR file not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Searched in:"
    Write-Host "  - target/ directory (development)"
    Write-Host "  - current directory (distribution)"
    Write-Host "  - parent directory"
    Write-Host ""
    Write-Host "If running from source, please run '.\scripts\build.ps1' first to build the project"
    Write-Host "If running from distribution, ensure the JAR file is in the same directory as this script"
    exit 1
}

# Determine if interactive or non-interactive mode
$Interactive = ($Password -eq "" -or $MasterKey -eq "")

if (-not $Interactive) {
    # Non-interactive mode - password and master key provided
    $javaArgs = @("-cp", $JarFile, "com.solace.psg.util.PasswordEncryptionCLI", $Command, $Password, $MasterKey)
    & java $javaArgs
} else {
    # Interactive mode
    $javaArgs = @("-cp", $JarFile, "com.solace.psg.util.PasswordEncryptionCLI", $Command)
    & java $javaArgs
}

