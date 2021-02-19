@echo off
pushd "%~dp0..\out"
for /f %%i in ('jdeps --print-module-deps artifacts\HD6Tools_jar\HD6Tools.jar') do set MODULES=%%i
rmdir /s /q distrib
mkdir distrib
jlink --no-header-files --no-man-pages --compress=2 --strip-debug --add-modules %MODULES% --output distrib\java-distrib
copy artifacts\HD6Tools_jar\HD6Tools.jar distrib\java-distrib\bin\HD6Tools.jar
echo @echo off> distrib\HD6Tools.bat
echo java-distrib\bin\java -jar java-distrib\bin\HD6Tools.jar %%*>> distrib\HD6Tools.bat
popd
