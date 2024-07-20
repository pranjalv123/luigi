set -e

./gradlew build
docker build -t docker-registry.svc.43mar.io/luigi .
docker push docker-registry.svc.43mar.io/luigi
nomad job restart -reschedule luigi
nomad alloc logs -f -task luigi  -job luigi