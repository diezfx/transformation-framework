@echo off
SET SCRIPT_PATH=%~dp0
java -jar %SCRIPT_PATH%/target/edmm-cli-1.0.0-SNAPSHOT.jar %*
