# c++

## libcurl
```
//使用前需配置好libcurl

#include <stdio.h>
#include <stdlib.h>
#include <curl/curl.h>

const char proxyUserPwd[] = "yourProxyAccount:yourProxyPassword";    //按照 "代理账号:代理密码" 格式填入
const char url[] = "https://myip.ipip.net";        //填入目标url

void main(int argc, char* argv[]) {
    CURL* curl;
    CURLcode res;

    curl = curl_easy_init();
    if (curl) {
        curl_easy_setopt(curl, CURLOPT_PROXY, "http://malenia.iinti.cn:24000");    //HTTP
        //curl_easy_setopt(curl, CURLOPT_PROXY, "socks5://malenia.iinti.cn:24000");    //socks5
        curl_easy_setopt(curl, CURLOPT_URL, url);    //url
        curl_easy_setopt(curl, CURLOPT_PROXYUSERPWD, proxyUserPwd);
        curl_easy_setopt(curl, CURLOPT_PROXYAUTH, CURLAUTH_BASIC);
        curl_easy_setopt(curl, CURLOPT_CONNECTTIMEOUT, 3);
        curl_easy_setopt(curl, CURLOPT_TIMEOUT, 5);

        res = curl_easy_perform(curl);
        if (res != CURLE_OK) {
            fprintf(stderr, "curl_easy_perform() failed: %s\n", curl_easy_strerror(res));
        }

        curl_easy_cleanup(curl);
    }
}
```

## winsock
```
#include<WinSock2.h>
#include<stdio.h>
#include<string.h>
#pragma comment(lib,"ws2_32.lib")


char proxyAddr[] = "malenia.iinti.cn";    
UINT16 proxyPort = 24000;        //填入提取出来的端口号
char proxyUserPwd[] = "yourProxyAccount:yourProxyPassword";    //按照 "代理账号:代理密码" 格式填入


const char base[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
char *base64_encode(const char* data, int data_len);

void main()
{
    WSADATA wsaData;
    int err;

    SOCKET sock = INVALID_SOCKET;

    err = WSAStartup(MAKEWORD(2, 2), &wsaData);
    if (err != 0) {
        printf("WSAStartup failed with error: %d\n", err);
        return;
    }

    //初始化套接字
    sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock == INVALID_SOCKET) {
        printf("socket create failed with error: %ld\n", WSAGetLastError());
        WSACleanup();
        return;
    }

    SOCKADDR_IN sAddr;
    sAddr.sin_family = AF_INET;
    sAddr.sin_port = htons(proxyPort);
    sAddr.sin_addr.S_un.S_addr = inet_addr(proxyAddr);

    err = connect(sock, (SOCKADDR *)&sAddr, sizeof(SOCKADDR));
    if (err) {
        printf("Unable to connect to server\n");
        closesocket(sock);
        WSACleanup();
        return;
    }

    char *proxyUserPwd_base64 = base64_encode(proxyUserPwd, strlen(proxyUserPwd));    

    //写入http请求，更改对应的目标URL
    char req[1024];
    snprintf(req, 1024, "GET http://myip.ipip.net HTTP/1.1\r\n\
Host: myip.ipip.net\r\n\
User-Agent: Mozilla/5.0\r\n\
Accept: */*\r\n\
Proxy-Connection: Keep-Alive\r\n\
Proxy-Authorization: Basic %s\r\n\
Connection: keep-alive\r\n\r\n", proxyUserPwd_base64);
    send(sock, req, strlen(req) + 1, 0);

    char recvbuf[1024];
    memset(recvbuf, NULL, sizeof(recvbuf));
    recv(sock, recvbuf, sizeof(recvbuf), 0);
    printf("%s\n", recvbuf);

    free(proxyUserPwd_base64);
    closesocket(sock);
    WSACleanup();
}


char *base64_encode(const char* data, int data_len)
{
    int prepare = 0;
    int ret_len;
    int temp = 0;
    char *ret = NULL;
    char *f = NULL;
    int tmp = 0;
    char changed[4];
    int i = 0;
    ret_len = data_len / 3;
    temp = data_len % 3;
    if (temp > 0)
    {
        ret_len += 1;
    }
    ret_len = ret_len * 4 + 1;
    ret = (char *)malloc(ret_len);

    if (ret == NULL)
    {
        printf("No enough memory.\n");
        exit(0);
    }
    memset(ret, 0, ret_len);
    f = ret;
    while (tmp < data_len)
    {
        temp = 0;
        prepare = 0;
        memset(changed, '\0', 4);
        while (temp < 3)
        {
            //printf("tmp = %d\n", tmp); 
            if (tmp >= data_len)
            {
                break;
            }
            prepare = ((prepare << 8) | (data[tmp] & 0xFF));
            tmp++;
            temp++;
        }
        prepare = (prepare << ((3 - temp) * 8));
        //printf("before for : temp = %d, prepare = %d\n", temp, prepare); 
        for (i = 0; i < 4; i++)
        {
            if (temp < i)
            {
                changed[i] = 0x40;
            }
            else
            {
                changed[i] = (prepare >> ((3 - i) * 6)) & 0x3F;
            }
            *f = base[changed[i]];
            //printf("%.2X", changed[i]); 
            f++;
        }
    }
    *f = '\0';

    return ret;
}
```