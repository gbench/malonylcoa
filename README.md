# malonylcoa

#### 介绍
丙二酰辅酶A是一种辅酶A衍生物，可用于脂肪酸和聚酮合成，以及胯线粒体膜α-转移酮戊二酸。丙二酰辅酶A是由乙酰辅酶A羧化酶介导的羧化反应生成。葡萄糖代谢也会产生丙二酰辅酶A。它会变构阻断肉碱棕榈酰转移酶1的作用，从而影响长链脂肪酸向线粒体的转移。脂肪酸合酶失活会导致丙二酰辅酶A过量，导致厌食。

#### 软件架构
软件架构说明


#### 安装教程

1.  xxxx
2.  xxxx
3.  xxxx

#### 使用说明

jshell --class-path D:/sliced/mvn_repos/gbench/pubchem/malonylcoa/0.0.1-SNAPSHOT/malonylcoa-0.0.1-SNAPSHOT.jar
``` java
import gbench.util.array.*
import static gbench.util.lisp.Lisp.*
import static gbench.util.array.INdarray.*
cph(RPTA(nats(2).data(),10)).map(INdarray::nd).map(INdarray::dupdata).collect(ndclc()).pivotTable(INdarray::length,nats(10).reverse().head(4).fmap(i->(Function<INdarray<Integer>,Integer>)nd->nd.get(i)))
```
#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


#### 特技

1.  使用 Readme\_XXX.md 来支持不同的语言，例如 Readme\_en.md, Readme\_zh.md
2.  Gitee 官方博客 [blog.gitee.com](https://blog.gitee.com)
3.  你可以 [https://gitee.com/explore](https://gitee.com/explore) 这个地址来了解 Gitee 上的优秀开源项目
4.  [GVP](https://gitee.com/gvp) 全称是 Gitee 最有价值开源项目，是综合评定出的优秀开源项目
5.  Gitee 官方提供的使用手册 [https://gitee.com/help](https://gitee.com/help)
6.  Gitee 封面人物是一档用来展示 Gitee 会员风采的栏目 [https://gitee.com/gitee-stars/](https://gitee.com/gitee-stars/)
