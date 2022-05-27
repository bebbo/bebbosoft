@echo off
for /f %%i in ("%0") do set y=%%~dpi
java -cp %y%target\classes;%y%..\security\target\classes;%y%..\util\target\classes Certify %*
