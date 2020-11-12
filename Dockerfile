FROM java:8-jre-alpine

LABEL maintainer="Cristian Lucchesi <cristian.lucchesi@iit.cnr.it>"

WORKDIR /client

RUN apk add --update bash && \
    mkdir conf && \
    rm -rf /var/cache/apk/*

ADD build/libs/epas-client-sql-1.0-all.jar \
	badStampings.sh \
    stampings.sh \
    test.sh \
    ./

ADD docker_conf ./docker_conf

CMD ["/client/docker_conf/init"]
