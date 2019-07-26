set APP_NAME=%1
set INPUT=%2
set OUTPUT=%3
set JAVA_RUNTIME=%4
set JAR=%5
set VERSION=%6
set APP_ICON=%7

call "%JAVA_HOME%\bin\java.exe" ^
    -Xmx512M ^
    --module-path "%JAVA_HOME%\jmods" ^
    --add-opens jdk.jlink/jdk.tools.jlink.internal.packager=jdk.packager ^
    -m jdk.packager/jdk.packager.Main ^
    create-image ^
    --verbose ^
    --echo-mode ^
    --name "%APP_NAME%" ^
    --main-jar "%JAR%" ^
    --version "%VERSION%" ^
    --input "%INPUT%" ^
    --output "%OUTPUT%" ^
    --runtime-image "%JAVA_RUNTIME%" ^
    --icon "%APP_ICON%"  
