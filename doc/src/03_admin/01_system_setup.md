# 系统安装

Malenia 系统支持三种安装方式，分别兼顾快速上手、扩展定制、性能优化等不同场景

## 裸机部署

请注意，malenia 是一个需要高度性能调优的网络程序，在生产环境我们更加建议使用裸机部署的方式，而**不建议使用 k8s、docker 等容器环境**,
malenia 隐藏在内部的组件和模块其实非常复杂，在您生产使用的时候，有可能需要联系我们进行一些性能调优、问题排查等实际需求，此时使用 docker 环境将会严重缺少相关工具，
这将导致无法最大化利用物理机器性能、无法排查某些场景的运行时 bug 等问题。

### 安装准备

- [下载安装包](https://oss.iinti.cn/malenia/MaleniaMain.zip)
- 安装 jdk1.8+（如果有可能，建议用户选择 jdk11 以上，将会具有更好的 GC 性能）
- 安装 mysql，或者购买 mysql 服务

### JDK 安装

#### Ubuntu

```shell
sudo apt-get install openjdk-11-jdk curl wget nload htop net-tools lsof unzip -y;
```

#### CentOS

```shell
yum -y install epel-release;
yum install java-11-openjdk* -y;
yum -y install curl wget nload htop net-tools lsof unzip;

```

#### 调整文件最大打开数

- linux 系统需要调整文件最大打开数，否则可能在高并发情况下出现连接失败的情况

```shell
$ vi /etc/security/limits.conf

# 添加如下内容

*               -    nproc          131072
*               -    nofile         131072

```

### 配置和初始化

支持 Windows/Linux 和 MacOS，最低要求 2G 内存，作为代理服务器建议带宽至少选择 4 兆。

- 解压安装包
- 数据库配置初始化配置在:`assets/ddl.sql`,请根据本 sql 文件进行数据库建表初始化
- conf 文件夹的相关配置
  - 项目使用 springboot，其中项目可选配置在 `conf/application.properties`，请在这里配置您的数据库链接信息（数据库为您上一步完成的 mysql 安装和数据库配置）
  - `conf/static/*`为前端资源，如果你想替换前端网页皮肤，则可以替换这里的内容 **malenia 前端是开源的，支持二开的**
  - `conf/static/malenia-doc/*`为文档资源，如果你想修改文档内容，则可以编辑这里
- 如果你需要完整的支持 mitm 功能的话，你需要额外的注意一下环境
  - brotli（br）压缩算法
    - windows: 请安装微软 c++运行时（Microsoft Visual C++ Redistributable）
    - 其他 os：无此要求
  - alpn(支持 http2.0)
    - 当 jdk=oracle jdk 1.8 时，需要安装 open ssl

### 启动和运行

- 执行`bin/startup.sh` (如果是 windows，那么执行 xxx.bat 即可)
- 观察日志是否正常
- 网站第一个完成注册的账户会自动变成管理员，其他后续账户都会被设定为普通账户，您后续可以在操作页面设定其他管理员。
- 系统默认配置 embed 代理服务器，以及使用 embed 代理服务器配置默认产品，用户可以使用内置模拟资源完成配置参考。也可以在已有配置上进行修改
- 系统默认为第一个管理员进行初始化充值

### 分布式部署

分布式部署流程非常简单，只需要将您刚编辑好的安装包文件夹复制到多台服务器并完成启动即可。但是需要注意您的 mysql 多台服务器鉴权。
分布式环境要求 malenia 系统的多台服务器都运行在公网并且可以相互通信。

### https 网站（TLS）

如下场景，你可能需要考虑让 malenia 部署在 https 网站下

- 为了网站安全，将 malenia 的后台网站部署在 https 站点上
- 为了流量安全，启用`http(s) proxy over TLS`能力： `curl -x https://test:test1@malenia.iinti.cn:9090 https://www.baidu.com/`
- 为了流量安全，启用`Socks5 over TLS`能力，**请注意，普通 socks 客户端（如 curl）不支持本功能，考虑国家法律要求，请自行寻找相关资料**

在使用 TLS 的情况下，需要我们申请 https 证书并且绑定到系统，malenia 提供了两种接入方案：

#### 购买标准的 CA 机构证书

此情况 CA 证书由国际受信 CA 机构颁发，将会被您的操作系统自动底层证书链完成验证，属于标准的接入方法，
请将申请到的 ssl 证书放到`conf`目录下， 并修改配置文件: `appliation.properties`

```properties
ssl.keystore.file=classpath:ssl_key_store_file.pfx
ssl.keystore.type=PKCS12
ssl.keystore.password=the-password
```

- 文件可以填写绝对文件路径
- 按照证书实际情况，密码可能为空


#### 使用 malenia 自动生成证书

在 CA 机构申请证书一般需要花钱，如果不想花钱，则可以通过 malenia 自动生成 https 证书，此时 malenia 代替了 CA 机构的身份。
由于操作系统不会内置 manenia 的根证书，所以此方案需要您手动在你的客户端主机上安装 malenia 根证书,[点击连接下载](/malenia-api/user-op/downloadCaCertificate?pem=true)

完成安装之后，需要在后台配置域名列表

## 一个环境冲突问题

为了对 http2.0 进行 mitm，我们需要使用 TLS ALPN 协议，但是这个协议在 oracle jdk1.8 的实现并不完整。 这会导致：《oracle jdk && <=1.8 && mitm &&
https》组合的 https 请求转发失败。

你可以做以下任何一种策略绕过这个问题：

1. 使用 openJDK
2. 使用 jdk 1.9+
3. 确保你的主机环境安装了完整的 openSSL 库，malenia 将会尝试使用 openSSL 方案

## docker-all-in-one

如果您想快速体验 malenia，可以通过如下脚本一键拉起 malenia，**请注意，原则上不建议此中部署方式上生产**，一键拉起的系统和手动部署的系统其功能和行为保持一致，
但是其性能和稳定性无法做到最优

```shell
# 安装docker
yum install -y docker
# 下载镜像:
docker pull registry.cn-beijing.aliyuncs.com/iinti/common:malenia-all-in-one-latest;
# 启动sekiro服务器
docker run -d -p 5810:8060 -p 9090-9095:9090-9095 -v ~/malenia-mysql-data:/var/lib/mysql --name malenia-all-in-one registry.cn-beijing.aliyuncs.com/iinti/common:malenia-all-in-one-latest
```

## docker-compose

如果您不太了解 java，也可以使用 docker-compose 的方式进行部署，

**此中部署方式可以支持多个节点的分布式，但是您需要手动修改部分`docker-compose.yaml`定义**

### 安装 docker-compose 环境

**已经安装过 docker 和 docker-compose 的请略过此步骤**

```shell
# 如果你的服务器长期没有更新，那么建议更新一下
sudo yum clean all
sudo yum makecache
sudo yum update

# 安装docker(系统依赖升级到最新，这里一般不会报错，如果报错请走一遍第一步，可以考虑使用阿里云的yum镜像)
sudo yum install -y yum-utils  device-mapper-persistent-data  lvm2
sudo yum-config-manager  --add-repo   https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install docker-ce docker-ce-cli containerd.io

# 设置docker开机自启动
sudo systemctl start docker
sudo systemctl enable docker
systemctl enable docker.service
systemctl start docker.service

# 安装docker-compose，这里使用了pip的方式
sudo pip install docker-compose
```

### 启动 malenia

执行命令:

```shell
curl https://oss.iinti.cn/malenia/quickstart.sh | bash
```

访问网站：[http://127.0.0.1:8060/](http://127.0.0.1:8060/),首次打开网站请注册账户，第一个注册账户将会成为管理员

- 请预先安装好`docker`、`docker-compose`
- 服务依赖 MySQL 启动，使用 Docker-compose 首次启动数据库较慢，
- 可能会 malenia-server 启动失败， 这种情况确认数据库启动成功后，直接 docker restart malenia-server
