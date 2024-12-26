# 环境闭包
envclos <- \(x=sys.frame(sys.nframe()), ret=list()) if(identical(emptyenv(), x)) ret else parent.env(x) |> envclos(append(x, ret));

# 数据全排列， ps: points点集合 gn: group numer 分组阶数 
pgn <- function(ps=1:3, gn=length(ps)) rep(ps, gn) |> split( rep(paste0("x", seq(gn)), rep(length(ps), gn)) ) |> expand.grid()
