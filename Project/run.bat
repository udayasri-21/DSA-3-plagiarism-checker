@echo off
if not exist out mkdir out
javac -d out src\*.java
if errorlevel 1 pause & exit /b
java -cp out Server 8080
pause
