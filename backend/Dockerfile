FROM ubuntu:18.04
RUN mkdir /backend
WORKDIR /backend
ADD . /backend

# install java and gradle
RUN apt-get update && \
    apt-get install -y openjdk-11-jdk gradle && \
    apt-get clean
