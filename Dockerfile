FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:1c7adc60725abb7a8a6d4b0e770d2a104a1a43fe94c8da010e736cc9f24f693d
ENV TZ="Europe/Oslo"
COPY build/libs/app.jar app.jar
CMD ["-jar","app.jar"]