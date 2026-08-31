@echo off
cd /d "C:\Users\Dev Pro\Documents\GitHub\ninetynine"
set "JAVA_HOME=C:\Users\Dev Pro\.jdks\jdk-17.0.19+10"
call gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain > build-verify.log 2>&1
echo GRADLE_EXIT:%errorlevel% >> build-verify.log
