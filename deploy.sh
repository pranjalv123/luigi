set -e

./gradlew build
docker build -t docker-registry.svc.43mar.io/luigi .
docker push docker-registry.svc.43mar.io/luigi
#docker build -t ghcr.io/pranjalv123/luigi .
#gh auth token |  docker login ghcr.io -u pranjalv123 --password-stdin
docker push ghcr.io/pranjalv123/luigi

nomad job restart -reschedule luigi
