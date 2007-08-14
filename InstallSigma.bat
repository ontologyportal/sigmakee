@echo off

echo.
echo Installing Sigma files ...
echo.

set EXPECTED_TOMCAT_VERSION=5
set EXPECTED_TOMCAT_ROOT=C:\Program Files\Apache Software Foundation\
set TOMCAT_ROOT=
set TOMCAT_KBs_DIR=
set TOMCAT_WEBAPPs_DIR=

for /D %%R in ( "%EXPECTED_TOMCAT_ROOT%Tomcat*%EXPECTED_TOMCAT_VERSION%*" ) do (
  set TOMCAT_ROOT=%%R
)

if not exist "%TOMCAT_ROOT%" (
  echo.
  echo Tomcat is not in %EXPECTED_TOMCAT_ROOT%.
  echo.
  echo Please install Tomcat or edit %0, and then try again.
  pause
  goto quit
)

set TOMCAT_WEBAPPs_DIR=%TOMCAT_ROOT%\webapps

if not exist "%TOMCAT_WEBAPPs_DIR%" (
  echo.
  echo The directory %TOMCAT_WEBAPPs_DIR% could not be found.
  pause
  goto quit
)

set TOMCAT_KBs_DIR=%TOMCAT_ROOT%\KBs

if not exist "%TOMCAT_KBs_DIR%" (
  mkdir "%TOMCAT_KBs_DIR%"
  if not exist "%TOMCAT_KBs_DIR%" (
    echo.
    echo The directory %TOMCAT_KBs_DIR% could not be created.
    echo.
    echo Perhaps you do not have permission to create it.
    pause
    goto quit
  )
)

if exist "%TOMCAT_ROOT%\work\Catalina\localhost\sigma" (
  rmdir /S /Q "%TOMCAT_ROOT%\work\Catalina\localhost\sigma"
  if exist "%TOMCAT_ROOT%\work\Catalina\localhost\sigma" (
    echo.
    echo Could not remove %TOMCAT_ROOT%\work\Catalina\localhost\sigma.
    echo.
    echo Please remove it before starting Tomcat.
  ) else (
    echo.
    echo Removed %TOMCAT_ROOT%\work\Catalina\localhost\sigma.
  )
)

if exist "%TOMCAT_WEBAPPs_DIR%\sigma" (
  rmdir /S /Q "%TOMCAT_WEBAPPs_DIR%\sigma"
  if exist "%TOMCAT_WEBAPPs_DIR%\sigma" (
    echo.
    echo Could not remove %TOMCAT_WEBAPPs_DIR%\sigma.
    echo.
    echo Please remove it before starting Tomcat.
  ) else (
    echo.
    echo Removed %TOMCAT_WEBAPPs_DIR%\sigma.
  )
)

if exist "%TOMCAT_WEBAPPs_DIR%\sigma.war" (
  copy "%TOMCAT_WEBAPPs_DIR%\sigma.war" "%TOMCAT_WEBAPPs_DIR%\sigma.war.old"
  if exist "%TOMCAT_WEBAPPs_DIR%\sigma.war.old" (
    echo Created sigma.war.old
    del "%TOMCAT_WEBAPPs_DIR%\sigma.war"
  )
)

for %%X in ( "sigma*.war" ) do (
  copy "%%X" "%TOMCAT_WEBAPPs_DIR%\sigma.war"
  if not exist "%TOMCAT_WEBAPPs_DIR%\sigma.war" (
    echo.
    echo Could not copy %%X.
    pause
    goto quit
  )
  echo Wrote %%~nxX.
)

for %%I in ( "KBs\*.kif" "KBs\*.txt" "KBs\*.exc" "KBs\index.*" ) do (
  if exist "%TOMCAT_KBs_DIR%\%%~nxI" (
    copy "%TOMCAT_KBs_DIR%\%%~nxI" "%TOMCAT_KBs_DIR%\%%~nxI.old"
    echo Created %%~nxI.old
    del "%TOMCAT_KBs_DIR%\%%~nxI"
  )
  copy "%%I" "%TOMCAT_KBs_DIR%\%%~nxI"
  if not exist "%TOMCAT_KBs_DIR%\%%~nxI" (
    echo.
    echo %%~nxI could not be copied.
    pause
    goto quit
  )
  echo Wrote %%~nxI.
)

set CFGFILE=%TOMCAT_KBs_DIR%\config.xml
if exist "%CFGFILE%" (
  copy "%CFGFILE%" "%CFGFILE%.old"
  echo Created config.xml.old
)

for %%X in ( "inference\*.exe" ) do (
  if exist "%%~fX" (
    echo ^<configuration^> > "%CFGFILE%"
    echo    ^<preference value^="%TOMCAT_ROOT%" name^="baseDir" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="%TOMCAT_KBs_DIR%" name^="kbDir" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="%%~fX" name="inferenceEngine" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="%TOMCAT_KBs_DIR%\tests" name^="inferenceTestDir" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="%TOMCAT_KBs_DIR%\tests" name^="testResultsDir" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="yes" name^="typePrefix" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="no" name^="holdsPrefix" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="no" name^="cache" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="yes" name^="TPTP" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="" name^="celtdir" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="no" name^="loadCELT" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="yes" name="showcached" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="" name^="lineNumberCommand" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="" name^="editorCommand" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="localhost" name^="hostname" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="8080" name^="port" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="SUMO" name^="sumokbname" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="" name^="prolog" ^/^> >> "%CFGFILE%"
    echo    ^<kb name^="SUMO"^> >> "%CFGFILE%"
    echo      ^<constituent filename^="%TOMCAT_KBs_DIR%\Merge.kif" ^/^> >> "%CFGFILE%"
    echo      ^<constituent filename^="%TOMCAT_KBs_DIR%\english_format.kif" ^/^> >> "%CFGFILE%"
    echo    ^<^/kb^> >> "%CFGFILE%"
    echo ^<^/configuration^> >> "%CFGFILE%"
  ) else (
    copy "KBs\config.xml" "%TOMCAT_KBs_DIR%\config.xml"
  )
)

if not exist "%CFGFILE%" (
  echo.
  echo config.xml could not be copied.
  pause
  goto quit
)
echo Wrote config.xml.

echo.
echo Sigma files installed!
echo.

pause

:quit
