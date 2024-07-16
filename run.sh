set -eu -o pipefail
./gradlew build
consul connect proxy -service op-pranjal -upstream mosquitto:1883 &
consul agent -data-dir=/tmp/consul/ -retry-join=consul-server-1.43mar.io -grpc-port=8502 -grpc-tls-port=8503 &
./gradlew run