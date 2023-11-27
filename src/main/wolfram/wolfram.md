#wolfram

## 简单语句
| 符号 | 说明 |
|----|----|
| `Module[{x:=1},x]`| 1|
| `Module[{x:={3,4}},Sqrt[x.x]]` | 1|
| `N| 数值类型，获取前面4位数字 ` | |
| `N[Pi,4]|3.141 显示为 3.142` | |
| `" "引号括起来的数字| "你好"` | |
| `CurrentDate` | 时间类型|
| `CurrentDate[]` | Thu 16 Nov 2023 13:36:09GMT + 8|
| `MoonPhase[CurrentDate[],"Icon"]` | |
| `Graphics` | 图形|
| `Graphics[Disk[]]` | |
| `Graphics[Style[Disk[{0,0}],RGBColor[255,0,0]]]` | |
| `Graphics[Style[Circle[],RGBColor[255,0,0]]]` | |
| `Graphics3D[{Red,Sphere[{0,0,0},0.5],Blue,Cube[{1,0,0}],1}]` | |
| `Area[Sphere[{c1,c2,c3},r]]` | |
| `{}` | 列表类型 |
| `{1,2,2,3,3,3}` | 列表类型 |
| `[[]]` | 读取列表中数据的元素 |
| `{1,2,3,4}[[1]]` | 1 |
| `;;` | |
| `{{11,12},{21,22}}[[;; 1]]` | |
| `{{11,12},{21,22}}[[;; 2]]` | |
| `Module[{x:={1,2,3,4}},Table[x[[n]],{n,4}]]` | |
| `Module[{x={1,2,3,4}},Table[x[[";;" n]],{n,4}]]` | |
| `%` | |
| `Table[RandomInteger[10],10];%` | |
| `/.` | 替换 |
| `{f[1],g[2],f[2],f[6],g[3]}/.f[x_]->x+10` | |
| `{{"apple",1},{"grape",2},{"apple",3},{"grape",4}}/.{"apple",x_}->{"apple",x+10}` | {{"apple",11},{"grape",2},{"apple",13},{"grape",4}} |
| `/;  Condition patt/;test  lhs:>rhs/;test  lhs:=rhs/;test` | 规则替换，条件满足时候才替换 |
| `{5,4,1,3,2}/.{a___,b_,c_,d___}/;b>c->{a,c,b,d}` | {4,5,1,3,2} |
| `{6,-7,3,2,-1,-2}/.x_/;x<0->w` | {6,w,3,2,w,w} |
| `f[x_]:=g[x]/;x>0;{f[5],f[-6]}` | `{g[5],f[-6]}` | 


## 语言示例
统计物理
``` wolfram
Module[{ (*变量的定义*)
   INITIAL = 100, (* 初始粒子数量*)
   n = INITIAL,(* 初始粒子数量*)
   r := {0, 1} // UniformDistribution // RandomVariate ,(*随机数量*)
   }, NestList[ (* 递归循环 *)
   If[r <= #/INITIAL, # - 1, # + 1] &,
   INITIAL, (*初始粒子数量*)
   400 (*循环次数*)
   ] 
  ] // ListPlot
```

Simplify 简化
``` wolfram
Table[Sin[x⁄2]Cos[nx],{n,1,10}]/.Sin[x_]Cos[y_]->(Sin[x-y]+Sin[x+y])/2//(p"|->" Plus@@p)//Simplify
```

关联表
``` wolfram
Module[{kvps:=((x|->Rule@@x)/@(Table[x,{x,10}]//(x|-> Partition[x,2,1]))//Association)},kvps[3]]
```

多项式展开1
``` wolfram
Module[{
	n := 3, (*次数*)
	vars := {a,b,c}, (*变量*)
	kvps:= GroupBy[Tuples[vars,n],Sort], (*全排列*)
	values := Map[Length,kvps // Values], (*统计项目数量*)
	keys := (x|->Times@@x)/@ Keys[kvps], (*项目名称*)
	terms := MapThread[{x,y}|->x y,{keys,values}] (*拼接项目*)
},Plus @@ terms]
```
多项式展开2
``` wolfram
Expand[〖(a+b+c)〗^3]
```