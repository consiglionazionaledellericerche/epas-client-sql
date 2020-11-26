FROM openjdk:8-alpine as build
RUN apk add --update bash && \	
	rm -rf /var/cache/apk/*
WORKDIR /client 
COPY . .
RUN ./gradlew init shadowJar

FROM openjdk:8-alpine
LABEL maintainer="Cristian Lucchesi <cristian.lucchesi@iit.cnr.it>"

WORKDIR /client

RUN apk add --update bash && \
    mkdir conf && \
    rm -rf /var/cache/apk/*

COPY --from=build /client/build/libs/epas-client-sql-1.0-all.jar .

ADD badStampings.sh \
    stampings.sh \
    test.sh \
    ./

ADD docker_conf ./docker_conf

CMD ["/client/docker_conf/init"]
