FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:a62eba2192aa8c3c60eb8829b171cdc4ed7d66d7ee5edf228fc7b6c6d3488df0
ENV TZ="Europe/Oslo"
COPY build/libs/app.jar app.jar
CMD ["-jar","app.jar"]