@echo off
REM Load environment variables from .env file and run the application

if exist .env (
    echo.
    echo 📝 Loading environment variables from .env file...
    for /f "delims== tokens=1,*" %%A in (.env) do (
        if not "%%A"=="" (
            if not "%%A:~0,1%"=="#" (
                set "%%A=%%B"
            )
        )
    )
    echo ✅ Environment variables loaded
) else (
    echo ⚠️  .env file not found. Using system environment variables or defaults.
)

echo.
echo 🚀 Starting Load Board Backend...
call mvnw.cmd spring-boot:run

pause
