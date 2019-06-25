set ISXPATH="%PROGRAMFILES(X86)%\Inno Setup 5\iscc.exe"
if not exist %ISXPATH% set ISXPATH="%PROGRAMFILES%\Inno Setup 5\iscc.exe"
if not exist %ISXPATH% echo "Inno Setup compiler not found" && goto :EOF
set IWZ=mist.iss
%ISXPATH% "%~dp0%IWZ%"
:EOF