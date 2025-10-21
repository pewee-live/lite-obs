# lite-obs

<p align="center">
  <strong>一个基于 Spring Boot 的轻量级对象存储服务 (OSS)</strong>
</p>

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-1.8+-blue?logo=java&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring_Boot-2.5.12-brightgreen?logo=spring&logoColor=white">
  <img alt="GitHub" src="https://img.shields.io/github/license/pewee-live/lite-obs">
  <img alt="GitHub stars" src="https://img.shields.io/github/stars/pewee-live/lite-obs?style=social">
</p>

`lite-obs` 是一个为中小企业设计的轻量级对象存储解决方案。

它使用 Java 和 Spring Boot 构建，提供了一套完整的 RESTful API，并采用可插拔的存储引擎架构，允许你轻松对接多种后端存储。

管理人员或公司it人员可以通过此项目和自有存储轻松搭建统一风格的轻量级OSS服务,解决了多种异构存储架构下api不统一,文件分散无法检索的问题.

目前synologyEngine已经经过验证在一年内存储超过1亿以上文件;


## 核心功能

* **文件上传**: 支持大文件的分片上传、断点续传。
* **文件下载**: 支持高性能的合并下载和按分片下载,可生成临时链接并可以自定义临时链接的可访问次数。
* **文件管理**: 提供文件列表查询、元数据获取、文件删除等功能。
* **数据完整性**: 在上传过程中使用 CRC32 校验码确保数据完整性。
* **可插拔存储引擎**: 核心亮点！后端存储逻辑被抽象为 `Engine` 层。
* **多后端支持**: 已内置支持多种存储引擎：
    * `localStorageEngine` (本地文件系统)
    * `synologyEngine` (群晖 NAS)
    * `qiniuEngine` (七牛云  即将支持)
    * `bdPanEngine` (百度网盘)
    * `ugosEngine` (绿联NAS UGOSPRO 即将支持)

## 项目架构

`lite-obs` 的设计灵感来源于 MySQL 的 `Server/Engine` 架构，将服务层与存储层完全解耦。

* **`obs-server`**: 服务层。负责处理 REST API 请求、用户鉴权、文件逻辑管理（如分片合并、元数据记录等）。
* **`engine`**: 存储引擎层。这是一个抽象接口，`obs-server` 通过它来操作文件。你可以实现此接口来添加新的存储后端 (如 S3, HDFS 等)，而无需修改任何 `obs-server` 层的代码。


## 技术栈

* **核心框架**: Spring Boot 2.5.12, Spring MVC
* **语言**: Java 1.8
* **数据库**: MySQL
* **数据访问**: MyBatis, Druid, PageHelper
* **缓存/锁**: Redis (使用 Jedis 客户端，支持集群)
* **工具库**: Lombok, FastJSON, Apache Commons, Joda-Time, JWT
* **构建工具**: Gradle

## 快速开始

### 1. 依赖环境

在部署 `lite-obs` 之前，请确保你已准备好以下环境：

* JDK 1.8
* MySQL (用于存储文件元数据)
* Redis (用于分片上传的锁和状态管理)
* Gradle (用于构建)

### 2. 下载与构建

```bash
# 1. 克隆项目
git clone [https://github.com/pewee-live/lite-obs.git](https://github.com/pewee-live/lite-obs.git)
cd lite-obs

# 2. 修改配置
#    请打开 src/main/resources/application.properties
#    并根据下一章节的 "配置说明" 修改数据库、Redis和存储引擎的配置。

# 3. 使用 Gradle 构建
./gradlew build

# 4.执行sql
在src\main\resources\sql下存放了数据建模pdb文件,如果你使用的是mysql,可以直接执行obs.sql

# 5. 运行服务
java -jar build/libs/lite-obs-1.0.0-RELEASE.jar


服务启动后，默认将在 12000 端口监听。
```

### 3. 配置说明

以下是启动服务前必须修改的配置项, 你必须至少配置一个你想要使用的存储引擎。

```bash
# 1. 服务端口
server.port=12000

# 2. 数据库配置
spring.datasource.url=jdbc:mysql://192.168.1.1:3306/obs?characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=000000

# 3. Redis 集群配置
spring.redis.cluster.nodes=127.0.0.1:8080
spring.redis.password=000000

# 默认使用的存储引擎类型 (例如 1)
obs.defaultType=1

# --- 群晖 NAS (synologyEngine) ---
synologynas.authList[0].namespace=my-synology0
synologynas.authList[0].url=http://192.168.1.111:5000/
synologynas.authList[0].user=YOUR_NAS_USER
synologynas.authList[0].auth=YOUR_NAS_PASSWORD

synologynas.authList[1].namespace=test1
synologynas.authList[1].url=http://192.168.1.3:5000/
synologynas.authList[1].user=bbb
synologynas.authList[1].auth=456

# --- 七牛云 (qiniuEngine) ---
qiniu.ak=YOUR_QINIU_ACCESS_KEY
qiniu.sk=YOUR_QINIU_SECRET_KEY

# --- 百度网盘 (bdpanEngine) ---
bdpan.appid=YOUR_BAIDU_APP_ID
bdpan.appkey=YOUR_BAIDU_APP_KEY
bdpan.secretkey=YOUR_BAIDU_SECRET_KEY
bdpan.backUrl=https://myurl.com:50000/obs/bdpan/token


```

## 快速使用流程 (Usage Flow) 🚀

一个新系统接入并使用 `lite-obs` 服务的完整流程如下。

### 步骤 1: 注册系统 (获取凭证)

你首先需要为你的应用注册一个 "系统" 身份，以获取调用 API 所需的凭证。这是一个一次性的管理操作。

* **Endpoint**: `POST /obs/sys/register`
* **重要限制**: 此接口默认受到IP限制，仅允许**内网 IP** (如 `127.0.0.1`, `192.168.x.x`) 调用。
* **Request Body**: (示例, 请根据你的 `SysDto` 调整)
    ```json
    {
      "sysName": "我的电商系统",
      "owner": "dev-team"
    }
    ```
* **Success Response**:
    你会收到包含 `sysCode` 和 `secretKey` 的 `SysVo` 对象。这是你的永久凭证，请妥善保管。
    ```json
    {
      "sysAlias": "my-ecommerce-alias",
      "sysCode": "auto-gen-code-123456",
      "secretKey": "auto-gen-secret-789012"
    }
    ```

### 步骤 2: 获取 Access Token (登录)

在使用文件服务之前，你必须使用上一步的凭证获取一个临时的 `Access Token`。

* **Endpoint**: `POST /obs/sys/authorize`
* **Content-Type**: `application/x-www-form-urlencoded`
* **Request Params**:
    * `sysCode`: `auto-gen-code-123456`
    * `secretKey`: `auto-gen-secret-789012`
    * `grandType`: `client_credentials` (固定值)
* **Success Response**:
    你会收到一个 `Access Token`。这个 Token 在一段时间后会过期，过期后需要重新获取。
    ```json
    {
      "code": "00000",
      "msg": "操作成功",
      "data": "eyJhbGciOiJIUzI1NiJ9.eyJzeXN..." // 这里是你的 Token
    }
    ```

### 步骤 3: 调用文件服务 (携带 Token)

现在你可以调用所有的文件 API。在**每一个**请求中，都必须携带 `Authorization` 请求头。

* **Header**: `Authorization`
* **Value**: `<your_token>` 

**示例：上传文件**
* **Endpoint**: `POST /obs/logicfile/upload`
* **Headers**:
    * `Authorization`: `OBS eyJhbGciOiJIUzI1NiJ9.eyJzeXN...`
* **Form-Data**: `file` (文件内容)
* **Success Response**: API 将返回文件的唯一标识 `code`。

**示例：获取下载链接**
* **Endpoint**: `GET /obs/logicfile/queryDownloadInfo/{file-code}`
* **Headers**:
    * `Authorization`: `Bearer eyJhbGciOiJIUzI1NiJ9.eyJzeXN...`
* **Success Response**: 返回包含文件元数据和下载 URL 的 JSON 对象。

---


### 认证 API (SysController)

路径前缀: `/obs/sys`

#### 1. 注册系统
* **Endpoint**: `POST /obs/sys/register`
* **认证**: **内网 IP**
* **说明**: 注册一个新系统，返回 `sysCode` 和 `secretKey`。

#### 2. 获取访问 Token
* **Endpoint**: `POST /obs/sys/authorize`
* **认证**: `sysCode` + `secretKey` (作为 `x-www-form-urlencoded` 参数)
* **说明**: 获取用于 API 调用的 `Access Token`。

---

### 文件 API (LogicFileController)

路径前缀: `/obs/logicfile`
**通用认证**: 所有 `logicfile` 接口都需要在请求头中携带 `Authorization: Bearer <token>`。

#### 1. 预上传 (分片上传初始化)
* **Endpoint**: `POST /obs/logicfile/preupload`
* **认证**: `Authorization` Header
* **说明**: 注册文件信息，获取 `code`，用于后续分片上传。

#### 2. 上传文件 / 分片
* **Endpoint**: `POST /obs/logicfile/upload`
* **认证**: `Authorization` Header
* **说明**: 上传完整文件或单个分片。

#### 3. 获取下载信息
* **Endpoint**: `GET /obs/logicfile/queryDownloadInfo/{code}`
* **认证**: `Authorization` Header
* **说明**: 获取文件元数据和下载链接。

#### 4. 下载文件 (合并)
* **Endpoint**: `GET /obs/logicfile/download/merge/{code}`
* **认证**: 无 (链接本身是签名的或已授权)
* **说明**: 服务器实时合并所有分片返回。

#### 5. 下载文件 (分片)
* **Endpoint**: `GET /obs/logicfile/download/part/{code}`
* **认证**: 无 (链接本身是签名的或已授权)
* **说明**: 下载单个分片。

#### 6. 删除文件
* **Endpoint**: `POST /obs/logicfile/delete`
* **认证**: `Authorization` Header
* **Request Body**: `["code1", "code2"]`
* **说明**: 批量删除文件。

#### 7. 查询文件列表
* **Endpoint**: `POST /obs/logicfile/listFile`
* **认证**: `Authorization` Header
* **说明**: 根据条件查询文件列表。



##  API详细指南

见[OBS微服务接口文档.md](OBS微服务接口文档.md)

## 致谢 (Acknowledgements)

`lite-obs` 的部分功能实现参考或直接使用了以下项目的源码，特此感谢所有原作者的贡献：

* **[群晖java客户端]**
    * **项目/来源**: [SynoFileStationClient]
    * **作者**: [heavyrage] 
    * **源码链接**: `[https://github.com/heavyrage/SynoFileStationClient]`


## 许可证
本项目采用[LICENSE](LICENSE) 

