# 基础介绍
##  maleniaMitm和mitmproxy的对比

|项目|maleniaMitm|mitmproxy|
|---|---|---|
|语言|groovy(Java)|python|
|二级代理|天然支持二级代理池|设置为单一二级代理上游，或者编程手动确定上游代理源|
|协议|暂时只支持http/https（todo）|支持http2，websocket|
|服务器和集群|运行在服务器集群中，脚本存储在数据库中|大多单节点，编程代码混合在服务器中，不方便管理|
|web|暂时不支持|支持完善的可视化页面|

从使用体感来说，mitmproxy大多还是单一手机app或者单一浏览器的注入。因为一个mitmproxy实例只提供一个http代理服务，请求进入服务之后很难做到不同的手机的流量各自走不同的上级代理，
相互独立不影响。maleniaMitm则是对于代理ip池的功能扩展，他首先是一个代理ip池，可以控制每个请求来源走独立的代理服务。天然支持多节点群控。


## groovy
malenia系统使用java开发，大家知道java是一个编译型语言，正常情况下比较难的实现脚本扩展，所以我们选择java生态下的一个动态语言实现脚本功能。这个语言就是groovy，
关于groovy的知识大家可以参考w3c的教程：[https://www.w3cschool.cn/groovy/groovy_overview.html](https://www.w3cschool.cn/groovy/groovy_overview.html)
groovy也是Android编译工具链的构建脚本（不过现在已经开始转移为kotlin了），如果大家开发过Android那么应该对groovy相对有点熟悉。
不过groovy其实完全兼容java语法，大家完全可以使用java的写法书写脚本。但是需要注意这里的java语法只能使用到java1.8

### 获取mitm的groovy工程环境
为了方便大家本地书写脚本，我们提供了一个集成环境，让大家知道这个脚本所支持的所有函数、功能、样例等。我将整个脚本驱动的模块单独抽取成了一个maven项目，并且实际上malenia的运行时就是使用这个maven项目完成mitm功能挂载的。
你可以在这里[下载这个集成环境](../../../build-in-res/mitm-dsl-project.zip),这个环境是maven驱动的，建议使用idea软件打开。

集成环境中有大量的场景demo，并且附有注释。同时集成环境解释了整个mitm的扩展模块如何开发的，你可以在这里看到很多没有在文档中描述的更加详细的API（在你了解goovy这门语言之后）

### 关于groovy可以使用的lib库
mitm脚本可以使用的依赖库是有malenia框架指定，你不可以自行添加，但是作为管理员可以在启动malenia系统之前手动往malenia的jar目录增加库文件，但是这种做法我们不保证不会出现依赖冲突
目前脚本库可以引用的依赖有：

|库|版本|
|---|---|
|org.jsoup:jsoup|1.4.2|
|com.alibaba:fastjson|2.0.1|
|org.apache.commons:commons-lang3|3.12.0|
|commons-io:commons-io|2.8.0|
|com.google.guava:guava|31.1-jre|

## 引用资产文件
脚本中我们可能需要一些静态文件，比如注入js，修改图片，修改json等。我们禁用了脚本直接操作文件和网络的能力，这是如果需要使用静态文件资源，那么需要将文件上传到平台。
你可以在 管理后台 -> 中间人注入 -> 资产文件 中上传和管理资产文件。

**请注意如果是分布式部署了malenia系统，那么需要使用阿里oss或者亚马逊oss存储方案。否则分布式环境下多节点无法同步文件数据**