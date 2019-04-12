# eclipse中运行该项目
## 将项目转为maven项目
右键-->configure-->Convert To Maven Project

## 添加ojdbc6.jar到本地仓库
由于版权问题,maven未提供ojdbc,所以要自己添加到本地仓库中.
### 配置运行参数
右键-->Run As Maven Build...-->Goals中参数为:
install:install-file  -Dfile=D:/ojdbc6.jar  -DgroupId=oracle  -DartifactId=ojdbc -Dversion=6 -Dpackaging=jar -DgeneratePom=true

## 打包
右键-->Run As Maven Build-->如未配置运行参数,则会弹出配置页面,在Goals中输入package即可
