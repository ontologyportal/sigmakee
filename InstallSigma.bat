@echo off

echo.
echo Installing Sigma files ...
echo.

set EXPECTED_TOMCAT_ROOT=C:\Program Files\Apache Software Foundation\
set TOMCAT_ROOT=
set TOMCAT_WEBAPPs_DIR=
set SIGMA_ROOT=
set SIGMA_KBS_DIR=

echo SIGMA_HOME=%SIGMA_HOME%
echo.

echo CATALINA_HOME=%CATALINA_HOME%
echo.


if exist "%CATALINA_HOME%" (
  set TOMCAT_ROOT=%CATALINA_HOME%
) else (
  for /D %%R in ( "%EXPECTED_TOMCAT_ROOT%Tomcat*" ) do (
    set TOMCAT_ROOT=%%R
  )
)

if not exist "%TOMCAT_ROOT%" (
  echo.
  echo Tomcat is not in %TOMCAT_ROOT%.
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

if not "%SIGMA_HOME%"=="" (
  if not exist "%SIGMA_HOME%" (
    mkdir "%SIGMA_HOME%"
  )
  set SIGMA_ROOT=%SIGMA_HOME%
) else (
  set SIGMA_ROOT=%TOMCAT_ROOT%
)

if not exist "%SIGMA_ROOT%" (
  mkdir "%SIGMA_ROOT%"
  if not exist "%SIGMA_ROOT%" (
    echo.
    echo The directory %SIGMA_ROOT% could not be created.
    echo.
    echo Perhaps you do not have permission to create it.
    pause
    goto quit
  )
)

set SIGMA_KBS_DIR=%SIGMA_ROOT%\KBs

if not exist "%SIGMA_KBS_DIR%" (
  mkdir "%SIGMA_KBS_DIR%"
  if not exist "%SIGMA_KBS_DIR%" (
    echo.
    echo The directory %SIGMA_KBS_DIR% could not be created.
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
  copy /B "%TOMCAT_WEBAPPs_DIR%\sigma.war" "%TOMCAT_WEBAPPs_DIR%\sigma.war.old"
  if exist "%TOMCAT_WEBAPPs_DIR%\sigma.war.old" (
    echo Wrote sigma.war.old.
    del "%TOMCAT_WEBAPPs_DIR%\sigma.war"
  )
)

for %%X in ( "sigma*.war" ) do (
  copy /B "%%X" "%TOMCAT_WEBAPPs_DIR%\sigma.war"
  if not exist "%TOMCAT_WEBAPPs_DIR%\sigma.war" (
    echo.
    echo Could not copy %%X.
    pause
    goto quit
  )
  echo Wrote %%~nxX.
)

for %%I in ( "KBs\*.kif" "KBs\*.txt" "KBs\*.exc" "KBs\index.*" ) do (
  if exist "%SIGMA_KBS_DIR%\%%~nxI" (
    copy /B "%SIGMA_KBS_DIR%\%%~nxI" "%SIGMA_KBS_DIR%\%%~nxI.old"
    echo Wrote %%~nxI.old
    del "%SIGMA_KBS_DIR%\%%~nxI"
  )
  copy /B "%%I" "%SIGMA_KBS_DIR%\%%~nxI"
  if not exist "%SIGMA_KBS_DIR%\%%~nxI" (
    echo.
    echo %%~nxI could not be copied.
    pause
    goto quit
  )
  echo Wrote %%~nxI.
)

if not exist "%SIGMA_KBS_DIR%\tests" (
  mkdir "%SIGMA_KBS_DIR%\tests"
)

if not exist "%SIGMA_KBS_DIR%\tests" (
  echo.
  echo The directory %SIGMA_KBS_DIR%\tests could not be created.
  echo.
  echo The inference test files will not be copied.
) else (
  for %%I in ( "KBs\tests\*" ) do (
    if exist "%SIGMA_KBS_DIR%\tests\%%~nxI" (
      copy /B "%SIGMA_KBS_DIR%\tests\%%~nxI" "%SIGMA_KBS_DIR%\tests\%%~nxI.old"
      echo Wrote %%~nxI.old
      del "%SIGMA_KBS_DIR%\tests\%%~nxI"
    )
    copy /B "%%I" "%SIGMA_KBS_DIR%\tests\%%~nxI"
    if not exist "%SIGMA_KBS_DIR%\tests\%%~nxI" (
      echo.
      echo %%~nxI could not be copied.
    )
    echo Wrote %%~nxI.
  )
)

set CFGFILE=%SIGMA_KBS_DIR%\config.xml
if exist "%CFGFILE%" (
  copy /B "%CFGFILE%" "%CFGFILE%.old"
  echo Wrote config.xml.old
)

for %%X in ( "inference\*.exe" ) do (
  if exist "%%~fX" (
    echo ^<configuration^> > "%CFGFILE%"
    echo    ^<preference value^="%SIGMA_ROOT%" name^="baseDir" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="%SIGMA_KBS_DIR%" name^="kbDir" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="%%~fX" name="inferenceEngine" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="%SIGMA_KBS_DIR%\tests" name^="inferenceTestDir" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="%TOMCAT_WEBAPPS_DIR%\sigma\tests" name^="testOutputDir" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="yes" name^="typePrefix" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="no" name^="holdsPrefix" ^/^> >> "%CFGFILE%"
    echo    ^<preference value^="yes" name^="cache" ^/^> >> "%CFGFILE%"
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
    echo      ^<constituent filename^="%SIGMA_KBS_DIR%\Merge.kif" ^/^> >> "%CFGFILE%"
    echo      ^<constituent filename^="%SIGMA_KBS_DIR%\english_format.kif" ^/^> >> "%CFGFILE%"
    echo    ^<^/kb^> >> "%CFGFILE%"
    echo ^<^/configuration^> >> "%CFGFILE%"
  ) else (
    copy /B "KBs\config.xml" "%SIGMA_KBS_DIR%\config.xml"
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
