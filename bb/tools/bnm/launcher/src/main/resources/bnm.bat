@for %%i in (%0) do @set LAUNCHERPATH=%%~dpi
@if not ""=="%M2_REPO%" @set BNMDEF=-DM2_REPO=%M2_REPO% 
java %BNMDEF% -cp %LAUNCHERPATH% BnmLauncher --version=0.1.0 %*
