windows 安装haskell

# 安装chocolatey
https://chocolatey.org/install

# 安装ghc
choco install ghc

# 升级ghc
chco upgrade ghc

# 安装 haskell包
# 添加到 GHC 环境：如果你希望在任何 Haskell 项目中都能使用 random 包，
# 可以使用 cabal install --lib random 命令。--lib 标志会将库安装到一个可以被 GHC 环境识别的位置，让你在编写代码时能够直接导入和使用。
cabal install --lib random

# ghc-pkg describe base    描述包信息
C:\Users\Administrator>ghc-pkg describe base
name:                 base
version:              4.19.1.0
visibility:           public
id:                   base-4.19.1.0-6554
key:                  base-4.19.1.0-6554
license:              BSD-3-Clause

# ghc-pkg list 查看包信息
C:\Users\Administrator>ghc-pkg list
C:\tools\ghc-9.8.2\lib\package.conf.d
    Cabal-3.10.2.0
    Cabal-syntax-3.10.2.0
    Win32-2.13.4.0
    array-0.5.6.0
    base-4.19.1.0
    binary-0.8.9.1
    bytestring-0.12.1.0
    containers-0.6.8
    deepseq-1.5.0.0
    directory-1.3.8.1
    exceptions-0.10.7
    filepath-1.4.200.1
    (ghc-9.8.2)
    ghc-bignum-1.3
    ghc-boot-9.8.2
    ghc-boot-th-9.8.2
    ghc-compact-0.1.0.0
    ghc-heap-9.8.2
    ghc-prim-0.11.0
    ghci-9.8.2
    haskeline-0.8.2.1
    hpc-0.7.0.0
    integer-gmp-1.1
    mtl-2.3.1
    parsec-3.1.17.0
    pretty-1.1.3.6
    process-1.6.18.0
    rts-1.0.2
    semaphore-compat-1.0.0
    stm-2.5.2.1
    system-cxx-std-lib-1.0
    template-haskell-2.21.0.0
    text-2.1.1
    time-1.12.2
    transformers-0.6.1.0
    xhtml-3000.2.2.1

# 查看ghci 安装位置
C:\Users\Administrator>where ghci
C:\tools\ghc-9.8.2\bin\ghci.exe

#查看cabal 配置文件位置：
%appdata%\cabal\config

# %appdata%\cabal\config位置中配置了hackage镜像的位置
# 需要配置本地镜像就是增加repository项目
repository hackage.haskell.org
  url: http://hackage.haskell.org/

# 使用 cabal update 更新包信息
C:\Users\Administrator>cabal update
Downloading the latest package list from hackage.haskell.org
Package list of hackage.haskell.org has been updated.
The index-state is set to 2025-02-01T18:31:52Z.
To revert to previous state run:
    cabal v2-update 'hackage.haskell.org,2025-02-01T17:41:47Z'


# Package list 更新位置
C:\Users\Administrator\AppData\Local\cabal\packages\hackage.haskell.org


# %appdata%\cabal\config位置中 remote-repo-cache 配置本地包缓存的存放位置
remote-repo-cache: C:\Users\Administrator\AppData\Local\cabal\packages

# 查看 ghc 软件信息
choco info ghc
C:\Users\Administrator>choco info ghc
Chocolatey v2.4.2
ghc 9.8.2 [Approved]
 Title: GHC | Published: 6/12/2024
 Package approved as a trusted package on Jan 24 2025 00:55:34.
 Package testing status: Passing on Jun 12 2024 20:12:11.
 Number of Downloads: 820749 | Downloads for this version: 10430
 Package url https://community.chocolatey.org/packages/ghc/9.8.2
 Chocolatey Package Source: n/a
 Package Checksum: '0mymrloaHkbw4V+IkrU9BoWakENqyNDQ4mCR0MjvIZBUAShzxGcWR4l4UAohbyIS/7xvrb0gGUSGrdz+p9xCjw==' (SHA512)
 Tags: ghc haskell
 Software Site: https://www.haskell.org/ghc/
 Software License: https://www.haskell.org/ghc/license
 Software Source: https://www.haskell.org/ghc/
 Documentation: https://downloads.haskell.org/~ghc/9.8.2/docs/
 Mailing List: https://mail.haskell.org/cgi-bin/mailman/listinfo/haskell-cafe
 Issues: https://ghc.haskell.org/trac/ghc/
 Summary: GHC is a state-of-the-art, open source, compiler and interactive environment for the functional language Haskell.

# 处理问题: 安装带有./configure的包文件
# 1.安装 MSYS2
choco install msys2 -y

# 2.初始化 MSYS2
# 打开一个新的具有管理员权限的 PowerShell 或命令提示符，执行以下命令来更新 MSYS2 及其包数据库：
# 启动 MSYS2 的 Bash 环境，并执行 pacman -Syuu 命令来更新 MSYS2 及其所有包。
# 在 PowerShell 中，& 是调用操作符（Call Operator）。它的作用是将一个命令或脚本文件作为一个单独的进程来执行。
& 'C:\tools\msys64\usr\bin\bash.exe' -lc "pacman -Syuu"

#3. 安装必要的编译工具： 
# -l：这个参数表示以登录 shell 的方式启动 bash。当使用 -l 参数时，bash 会读取并执行用户的登录脚本（如 .bash_profile 或 .profile），
#      加载必要的环境变量和配置信息，就像你在 Unix 系统中登录时一样。
# -c：-c 参数后面需要跟一个字符串，这个字符串就是要在 bash 环境中执行的命令。bash 会将 -c 后面的字符串作为一个命令来执行，执行完毕后就退出。
# pacman：MSYS2 所使用的包管理器，类似于 Linux 系统中的 apt 或 yum，用于安装、更新和管理软件包。
# base-devel：这是一个元包，它包含了一组基本的开发工具，如 make、gcc、autoconf 等，这些工具在编译和构建软件时经常会用到。
# mingw-w64-x86_64-toolchain：这是一个用于 64 位 Windows 系统的 MinGW-w64 工具链包，包含了用于编译 C、C++ 等语言的编译器和相关工具。
& 'C:\tools\msys64\usr\bin\bash.exe' -lc "pacman -S --needed base-devel mingw-w64-x86_64-toolchain"

#4. 配置环境变量
# 为了让 Windows 系统能够找到 MSYS2 中的 sh 等命令
# 需要将 MSYS2 的 bin 目录添加到系统的环境变量 PATH 中。在 PowerShell 中，$env: 是一个特殊的前缀，用于访问和操作环境变量。
# [Environment]::SetEnvironmentVariable：这是一个 .NET 类的静态方法，用于设置环境变量。
# [EnvironmentVariableTarget] 是一个枚举类型，Machine 表示将环境变量的修改应用到整个系统，即对所有用户和所有进程都生效。
$env:PATH = $env:PATH + ";C:\tools\msys64\usr\bin"
[Environment]::SetEnvironmentVariable("PATH", $env:PATH, [EnvironmentVariableTarget]::Machine)

# 重新安装失败的文件包
cabal install network-3.2.7.0

