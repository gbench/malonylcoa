#' 计算起始环境对象x的各个环境闭包
#' @param x 起始的环境对象
#' @param ret 返回值，上一阶调用的返回值
#' @return 起始环境对象x的各个环境闭包
envclos <- \(x=sys.frame(sys.nframe()), ret=list()) if(identical(emptyenv(), x)) ret else parent.env(x) |> envclos(append(x, ret));

#' 查看所有变量符号所在的环境名称，当which='path'的时候就是查看符号所在的库文件路径，可以通过R.home() 查看R_HOME即R的的根安装位置目录
# envclos() |> (\(xs, nms=lapply(xs, partial(attr, which='name'))) structure(lapply(xs, names), names=nms)) ()

#' 数据descartes product
#' @param ps: points点集合 
#' @param gn: group numer 分组阶数 
pgn <- function(ps=1:3, gn=length(ps)) rep(ps, gn) |> split( rep(paste0("x", seq(gn)), rep(length(ps), gn)) ) |> expand.grid()
