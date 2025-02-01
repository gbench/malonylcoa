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