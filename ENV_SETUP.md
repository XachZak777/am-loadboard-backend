# IntelliJ IDEA Configuration for Loading .env Variables

## Setup Instructions

### Option 1: Using the provided run scripts (Recommended)

1. **Linux/Mac:**
   ```bash
   chmod +x run.sh
   ./run.sh
   ```

2. **Windows:**
   ```cmd
   run.bat
   ```

These scripts will automatically load the `.env` file before starting the application.

---

### Option 2: Configure IntelliJ Run Configuration

1. **Open Run → Edit Configurations**

2. **Select or Create the "LoadboardBackendApplication" configuration**

3. **Add a "Before launch" task:**
   - Click "+" button under "Before launch" section
   - Select "Run Another Configuration"
   - Create a new "Maven" configuration with these settings:
     - Name: `Load Env Variables`
     - Command line: `process-resources`
     - Working directory: `$ProjectFileDir$`

4. **Alternative: Use Environment Variables directly in run config:**
   - In the run configuration, go to "Environment variables"
   - Click the button to load from file
   - Select `.env` file
   - IntelliJ will parse and load all variables

5. **Set VM options (if needed):**
   ```
   -Dspring.config.location=classpath:/application.properties,file:./.env
   ```

---

### Option 3: IDE Plugin

Install the "EnvFile" or "DotEnv" plugin from JetBrains Marketplace:

1. Go to **Settings → Plugins → Marketplace**
2. Search for "EnvFile" or "DotEnv"
3. Install the plugin
4. In your run configuration, enable the plugin
5. Specify the `.env` file path

---

### Option 4: Maven Spring Boot Plugin (Automatic)

With the `spring-dotenv` plugin added to `pom.xml`, it will automatically load `.env` on:

```bash
./mvnw spring-boot:run
```

The plugin loads environment variables during the `initialize` phase.

---

## Verification

After starting the application, verify the environment variables are loaded:

```bash
# Check logs for confirmation
# Should see messages like:
# "Loading environment variables from .env file..."
# "Environment variables loaded"

# Verify in application
curl http://localhost:8080/actuator/health
```

If you see the application starting correctly with:
- Database connection successful
- JWT_SECRET validation passing
- No "Missing required environment variables" errors

Then the `.env` file was loaded successfully! ✅

---

## Notes

- The `.env` file is in `.gitignore` to prevent committing sensitive data
- Use `.env.example` as a template
- Never commit the `.env` file to version control
- For production, use environment variables set at the OS level or container platform

