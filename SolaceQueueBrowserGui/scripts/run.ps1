# SolaceQueueBrowserGui Run Script (PowerShell)
# Runs the application with a specified config file (defaults to config/default.json)
# Note: Users should create config/default.json by copying config/sample-config.json
# Supports master password for decrypting encrypted passwords

# Change to the project directory (parent of scripts/)
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location (Join-Path $ScriptDir "..")

Write-Host "=================================================="
Write-Host "Starting SolaceQueueBrowserGui"
Write-Host "=================================================="

# Parse command line arguments manually for compatibility with batch file
$ConfigFile = ""
$MasterPassword = ""
$ShowHelp = $false

$i = 0
while ($i -lt $args.Count) {
    $arg = $args[$i]
    
    if ($arg -eq "-c" -or $arg -eq "--config") {
        if ($i + 1 -lt $args.Count) {
            $ConfigFile = $args[$i + 1]
            $i += 2
        } else {
            Write-Host "Error: $arg requires a value" -ForegroundColor Red
            exit 1
        }
    }
    elseif ($arg -eq "-mp" -or $arg -eq "--master-password") {
        if ($i + 1 -lt $args.Count) {
            $MasterPassword = $args[$i + 1]
            $i += 2
        } else {
            Write-Host "Error: $arg requires a value" -ForegroundColor Red
            exit 1
        }
    }
    elseif ($arg -eq "-h" -or $arg -eq "--help") {
        $ShowHelp = $true
        $i++
    }
    else {
        # Legacy support: if first argument doesn't start with - or /, treat as config file
        if ($ConfigFile -eq "" -and $arg -notmatch "^-" -and $arg -notmatch "^/") {
            $ConfigFile = $arg
            $i++
        } else {
            Write-Host "Error: Unknown option: $arg" -ForegroundColor Red
            Write-Host "Use --help for usage information"
            exit 1
        }
    }
}

# Show help if requested
if ($ShowHelp) {
    Write-Host ""
    Write-Host "Usage: $($MyInvocation.MyCommand.Name) [options]"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -c, --config FILE              Configuration file (default: config/default.json)"
    Write-Host "  -mp, --master-password PWD     Master password for decrypting encrypted passwords"
    Write-Host "  -h, --help                     Show this help message"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  # Run with default config:"
    Write-Host "  .\run.ps1"
    Write-Host ""
    Write-Host "  # Run with specific config:"
    Write-Host "  .\run.ps1 -c config/local-dev.json"
    Write-Host ""
    Write-Host "  # Run with master password (for encrypted passwords):"
    Write-Host "  .\run.ps1 -c config/default.json --master-password 'myMasterKey'"
    Write-Host ""
    Write-Host "Note: Create config/default.json by copying config/sample-config.json and"
    Write-Host "      updating it with your specific broker connection details."
    Write-Host ""
    Write-Host "Important Notes:"
    Write-Host "  - Always quote the master password if it contains special characters"
    Write-Host "  - Special characters like #, `$, !, etc. must be quoted: --master-password 'pass#123'"
    Write-Host "  - If decryption fails, verify you're using the same master password used for encryption"
    Write-Host "  - Use interactive GUI prompt if command-line password handling is problematic"
    Write-Host ""
    exit 0
}

# Set default config file if not provided
if ($ConfigFile -eq "") {
    $ConfigFile = "config/default.json"
    Write-Host "Using default config: $ConfigFile"
} else {
    Write-Host "Using provided config: $ConfigFile"
}

# Check if config file exists
if (-not (Test-Path $ConfigFile)) {
    Write-Host "Error: Config file '$ConfigFile' not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Available config files:"
    $configFiles = Get-ChildItem -Path "config" -Filter "*.json" -ErrorAction SilentlyContinue
    if ($configFiles) {
        $configFiles | ForEach-Object { Write-Host "  $($_.Name)" }
    } else {
        Write-Host "  No .json files found in config/"
    }
    Write-Host ""
    Write-Host "Usage: $($MyInvocation.MyCommand.Name) [options]"
    Write-Host "  Use --help for detailed usage information"
    exit 1
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

Write-Host "Starting application..."
Write-Host "   JAR: $JarFile"
Write-Host "   Config: $ConfigFile"
if ($MasterPassword -ne "") {
    Write-Host "   Master Password: [provided]"
}
Write-Host ""

# Build Java command arguments
$javaArgs = @("-jar", $JarFile, "-c", $ConfigFile)

if ($MasterPassword -ne "") {
    $javaArgs += "--master-password"
    $javaArgs += $MasterPassword
}

# Run the application
& java $javaArgs

