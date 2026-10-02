@echo off
setlocal
cd /d "%~dp0"

echo ==============================================
echo EXPERIMENT 7 - HADOOP MAPREDUCE
echo ==============================================

if not exist "target\mapreduce-exp7-1.0.jar" (
    echo JAR not found. Building the project...
    mvn clean package
    if errorlevel 1 (
        echo Maven build failed.
        pause
        exit /b 1
    )
)

if exist "output" rmdir /s /q "output"

echo.
echo Starting MapReduce Word Count...
echo.

java -jar "target\mapreduce-exp7-1.0.jar" "input" "output"

echo.
echo ==============================================
echo MAPREDUCE OUTPUT
echo ==============================================
if exist "output\part-r-00000" (
    type "output\part-r-00000"
) else (
    echo Output file was not generated.
)

echo.
pause
endlocal
