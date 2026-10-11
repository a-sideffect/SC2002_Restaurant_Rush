@echo off
if exist out rmdir /s /q out
mkdir out
javac --release 17 -encoding UTF-8 -d out -sourcepath src/main/java src/main/java/rush/Main.java || exit /b 1
echo Built into out folder
