@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\monitor.ps1" -Port COM9
pause