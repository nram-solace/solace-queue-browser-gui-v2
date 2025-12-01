# Runtime Distribution Package

This is a runtime-only distribution package of SolaceQueueBrowserGui. It contains only the files necessary to run the application, without source code or development files.

## Package Contents

- **Application JAR**: `SolaceQueueBrowserGui-1.0.0-jar-with-dependencies.jar` - Self-contained executable with all dependencies
- **Configuration Files**: 
  - `config/system.json` - Required system configuration
  - `config/log4j2.properties` - Logging configuration
  - `config/*.png` - Application icons and images
  - `config/default.json` - Sample user configuration template
  - `config/solace-cloud.json` - Sample Solace Cloud configuration template
- **Scripts**: 
  - `scripts/run.sh` - Main application launcher
  - `scripts/encrypt-password.sh` - Password encryption utility
  - `scripts/crypt-util.sh` - Cryptographic utilities
- **Documentation**: 
  - `README.md` - Project overview
  - `USER_GUIDE.md` - Complete user guide and reference
- **Runtime Directories**:
  - `downloads/` - Directory for downloaded messages (created automatically)
  - `logs/` - Directory for application logs (created automatically)

## Quick Start

1. Extract the distribution package:
   ```bash
   unzip SolaceQueueBrowserGui-1.0.0-runtime-distribution.zip
   cd SolaceQueueBrowserGui-1.0.0/
   ```

2. Configure your broker connection in `config/default.json` (or create your own config file)

3. Run the application:
   ```bash
   ./scripts/run.sh -c config/default.json
   ```

For detailed instructions, see `USER_GUIDE.md`.

## Requirements

- Java Runtime Environment (JRE) 17 or higher
- Network access to Solace broker SEMP API endpoint
- Network access to Solace broker messaging endpoint
- Appropriate credentials (SEMP admin and messaging client)

## What's NOT Included

This distribution package excludes:
- Source code (`src/` directory)
- Build artifacts (`target/` directory, except the final JAR)
- Development dependencies (`lib/` directory)
- Development configuration files (`local-dev.json`, `sampleBrowserConfig.json`)
- Build files (`pom.xml`)
- IDE configuration files
- Git repository files

## Building Your Own Distribution

To create a fresh distribution package from source:

```bash
cd SolaceQueueBrowserGui/
./scripts/build-distribution.sh
```

The distribution packages will be created in the `target/` directory:
- `SolaceQueueBrowserGui-1.0.0-runtime-distribution.zip`
- `SolaceQueueBrowserGui-1.0.0-runtime-distribution.tar.gz`

