FROM registry.cn-beijing.aliyuncs.com/iinti/ubuntu-openjdk:17
ENV TZ=Asia/Shanghai
WORKDIR /opt/malenia
COPY ./MaleniaMain.zip /opt/malenia/
RUN unzip MaleniaMain.zip
RUN rm -f MaleniaMain.zip
EXPOSE 8060
ENTRYPOINT [ "sh","./bin/MaleniaMain" ]
