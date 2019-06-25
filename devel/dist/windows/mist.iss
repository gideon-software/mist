; Inno Setup installer for MIST

#include "..\..\..\pack\appinfo.txt"

[Setup]
AppId={{#AppId}
AppName={#AppName}
ArchitecturesAllowed={#AppArch}
ArchitecturesInstallIn64BitMode=x64
AppVersion={#AppVersionLong}
VersionInfoVersion={#AppVersionLong}
AppVerName={#AppName} {#AppVersionShort}
AppSupportURL={#AppURL}
AppUpdatesURL={#AppURL}
AppCopyright={#AppCopyright}
AppPublisher={#AppPublisher}
DefaultDirName={pf}\{#AppName}
DefaultGroupName={#AppName}
SourceDir=..\..\..
OutputDir=dist
OutputBaseFilename={#AppName}-{#AppVersionLong}-{#AppDist}-setup
AllowNoIcons=no

[Tasks]
Name: "desktopicon"; Description: "On your &desktop"; GroupDescription: Additional shortcuts:
Name: "tntmenu"; Description: "In the &TntConnect ""Tools"" menu"; GroupDescription: Additional shortcuts: 

[Registry]
#define TntExternalToolsKey32 "SOFTWARE\TntWare\TntConnect\ExternalTools\MIST: Email Importer"
#define TntExternalToolsKey64 "SOFTWARE\Wow6432Node\TntWare\TntConnect\ExternalTools\MIST: Email Importer"
; 32-bit
Root: HKLM; Subkey: "{#TntExternalToolsKey32}"; Flags: uninsdeletekey; Tasks: tntmenu; Check: not IsWin64
Root: HKLM; Subkey: "{#TntExternalToolsKey32}"; ValueType: string; ValueName: "Category"; ValueData: "Add-Ons"; Tasks: tntmenu; Check: not IsWin64
Root: HKLM; Subkey: "{#TntExternalToolsKey32}"; ValueType: string; ValueName: "Target"; ValueData: "{app}\{#AppExeName}"; Tasks: tntmenu; Check: not IsWin64
Root: HKLM; Subkey: "{#TntExternalToolsKey32}"; ValueType: string; ValueName: "IconTarget"; ValueData: "{app}\mist.ico"; Tasks: tntmenu; Check: not IsWin64
; 64-bit
Root: HKLM; Subkey: "{#TntExternalToolsKey64}"; Flags: uninsdeletekey; Tasks: tntmenu; Check: IsWin64
Root: HKLM; Subkey: "{#TntExternalToolsKey64}"; ValueType: string; ValueName: "Category"; ValueData: "Add-Ons"; Tasks: tntmenu; Check: IsWin64
Root: HKLM; Subkey: "{#TntExternalToolsKey64}"; ValueType: string; ValueName: "Target"; ValueData: "{app}\{#AppExeName}"; Tasks: tntmenu; Check: IsWin64
Root: HKLM; Subkey: "{#TntExternalToolsKey64}"; ValueType: string; ValueName: "IconTarget"; ValueData: "{app}\mist.ico"; Tasks: tntmenu; Check: IsWin64

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "pack\*"; Excludes: "appinfo.txt"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppExeName}"; Comment: "{#AppComment}"; IconFilename: "{app}\mist.ico";
Name: "{group}\{#AppName} Homepage"; Filename: "{#AppURL}"
Name: "{group}\{cm:UninstallProgram,{#AppName}}"; Filename: "{uninstallexe}"
Name: "{userdesktop}\MIST"; Filename: "{app}\{#AppExeName}"; WorkingDir: "{app}"; Comment: "{#AppComment}"; IconFilename: "{app}\mist.ico"; Tasks: desktopicon;

[Run]
Filename: "{app}\{#AppExeName}"; Description: "{cm:LaunchProgram,{#AppName}}"; Flags: nowait postinstall skipifsilent

#include "uninstall.iss"
