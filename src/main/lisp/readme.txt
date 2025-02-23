# ---------------------------------------------------------------------------------
# 安装lisp
choco install sbcl

C:\Users\Administrator>choco install sbcl
Chocolatey v2.4.2
Installing the following packages:
sbcl
By installing, you accept licenses for the packages.
Downloading package from source 'https://community.chocolatey.org/api/v2/'
Progress: Downloading sbcl 2.5.1... 100%

sbcl v2.5.1 [Approved]
sbcl package files install completed. Performing other installation steps.
The package sbcl wants to run 'chocolateyinstall.ps1'.
Note: If you don't run this script, the installation will fail.
Note: To confirm automatically next time, use '-y' or consider:
choco feature enable -n allowGlobalConfirmation
Do you want to run the script?([Y]es/[A]ll - yes to all/[N]o/[P]rint): Y

WARNING: Url has SSL/TLS available, switching to HTTPS for download
Downloading sbcl
  from 'https://prdownloads.sourceforge.net/sbcl/sbcl-2.5.1-x86-64-windows-binary.msi'
Progress: 100% - Completed download of C:\Users\Administrator\AppData\Local\Temp\chocolatey\sbcl\2.5.1\sbcl-2.5.1-x86-64-windows-binary.msi (12.68 MB).
Download of sbcl-2.5.1-x86-64-windows-binary.msi (12.68 MB) completed.
Hashes match.
Installing sbcl...
sbcl has been installed.
Added C:\ProgramData\chocolatey\bin\sbcl.exe shim pointed to 'c:\program files\steel bank common lisp\sbcl.exe'.
  sbcl may be able to be automatically uninstalled.
Environment Vars (like PATH) have changed. Close/reopen your shell to
 see the changes (or in powershell/cmd.exe just type `refreshenv`).
 The install of sbcl was successful.
  Software installed as 'MSI', install location is likely default.

Chocolatey installed 1/1 packages.
 See the log for details (C:\ProgramData\chocolatey\logs\chocolatey.log).

Enjoy using Chocolatey? Explore more amazing features to take your
experience to the next level at
 https://chocolatey.org/compare

# ---------------------------------------------------------------------------------
# 启动sbcl
sbcl
C:\Users\Administrator>sbcl
This is SBCL 2.5.1, an implementation of ANSI Common Lisp.
More information about SBCL is available at <http://www.sbcl.org/>.

SBCL is free software, provided as is, with absolutely no warranty.
It is mostly in the public domain; some portions are provided under
BSD-style licenses.  See the CREDITS and COPYING files in the
distribution for more information.
* (+ 1 2)
3
* (+ 1 2 3 4 5)
15
*