@echo off
set JAVA_EXE="%~dp0jre\bin\javaw.exe"
set MIST_JAR="%~dp0lib\mist.jar"
start /B "MIST loading..." %JAVA_EXE% -jar %MIST_JAR% %*
