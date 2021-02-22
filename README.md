### 开发环境搭建
- base/util/cloud/download/networking按maven项目导入到ide；
- maven import，先base，再util/cloud/download/networking；
- mvn install，先base，再util；
- build，先util，再cloud/download/networking；
- 运行，cloud/download/networking程序入口***Application.java



### 服务部署
- base，运行命令mvn install；
- util，运行命令mvn install；
- cloud/download/networking，运行命令mvn package，生成各自可执行jar；
- 执行jar，运行命令java -jar ***.jar；