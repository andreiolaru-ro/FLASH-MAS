@echo off
setlocal

REM Check for the bundled JRE
IF EXIST ".\\jre\\bin\\java.exe" (
    ECHO Found bundled JRE.
    SET "JAVA_EXE=.\\jre\\bin\\java.exe"
) ELSE (
    ECHO Bundled JRE not found. Using system's java.
    SET "JAVA_EXE=java"
)

ECHO Starting Flash-MAS Daemon...
REM The %* passes all command-line arguments to the daemon
%JAVA_EXE% -jar FlashMasDaemon.jar %*

endlocal