# 环境闭包
envclos <- \(x=sys.frame(sys.nframe()), ret=list()) if(identical(emptyenv(), x)) ret else parent.env(x) |> envclos(append(x, ret));
