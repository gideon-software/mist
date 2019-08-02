; Inno Setup installer for MIST

#include "..\..\..\build\bundle\appInfo.txt"

[Setup]
AppId={{#AppId}
AppName={#AppName}
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64
AppVersion={#AppVersion}
VersionInfoVersion={#AppVersion4Dot}
AppVerName={#AppName} {#AppVersion}
AppSupportURL={#AppURL}
AppUpdatesURL={#AppURL}
AppCopyright={#AppCopyright}
AppPublisher={#AppPublisher}
DefaultDirName={autopf}\{#AppName}
DefaultGroupName={#AppName}
SourceDir=..\..\..
OutputDir=build\bundle
OutputBaseFilename={#AppName}-{#AppVersion}-{#AppDist}-setup
AllowNoIcons=no

[Tasks]
Name: "desktopicon"; Description: "On your &desktop"; GroupDescription: Additional shortcuts:
Name: "tntmenu"; Description: "In the &TntConnect ""Tools"" menu"; GroupDescription: Additional shortcuts: 

[Registry]
#define TntExternalToolsKey64 "SOFTWARE\Wow6432Node\TntWare\TntConnect\ExternalTools\MIST: Email Importer"
Root: HKLM; Subkey: "{#TntExternalToolsKey64}"; Flags: uninsdeletekey; Tasks: tntmenu;
Root: HKLM; Subkey: "{#TntExternalToolsKey64}"; ValueType: string; ValueName: "Category"; ValueData: "Add-Ons"; Tasks: tntmenu;
Root: HKLM; Subkey: "{#TntExternalToolsKey64}"; ValueType: string; ValueName: "Target"; ValueData: "{app}\{#AppExeName}"; Tasks: tntmenu;
Root: HKLM; Subkey: "{#TntExternalToolsKey64}"; ValueType: string; ValueName: "IconTarget"; ValueData: "{app}\mist.ico"; Tasks: tntmenu;

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "build\bundle\{#AppName}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppExeName}"; Comment: "{#AppComment}"; IconFilename: "{app}\mist.ico";
Name: "{group}\{#AppName} Homepage"; Filename: "{#AppURL}"
Name: "{group}\{cm:UninstallProgram,{#AppName}}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\MIST"; Filename: "{app}\{#AppExeName}"; WorkingDir: "{app}"; Comment: "{#AppComment}"; IconFilename: "{app}\mist.ico"; Tasks: desktopicon;

[Run]
Filename: "{app}\{#AppExeName}"; Description: "{cm:LaunchProgram,{#AppName}}"; Flags: nowait postinstall skipifsilent

#include "uninstall.iss"
