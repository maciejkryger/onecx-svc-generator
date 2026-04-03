FROM ghcr.io/onecx/docker-quarkus-native:0.3.0

COPY --chown=1001 target/*-runner /work/application