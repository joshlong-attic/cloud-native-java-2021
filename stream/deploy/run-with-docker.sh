
function read_kubernetes_secret() {
  kubectl get secret $1 -o jsonpath="{.data.$2}" | base64 --decode
}


cd `dirname $0`/..

mvn clean spring-boot:build-image

AN=$( docker images | grep rmq-app | cut -f1 -d\  )

RMQ_USER=$(read_kubernetes_secret bp-rabbitmq-secrets RABBITMQ_DEFAULT_USER)
RMQ_PW=$(read_kubernetes_secret bp-rabbitmq-secrets RABBITMQ_DEFAULT_PASS)

echo "${RMQ_USER} ${RMQ_PW}"

docker run \
  -e SPRING_RABBITMQ_HOST=host.docker.internal  \
  -e SPRING_RABBITMQ_USERNAME="${RMQ_USER}"  \
  -e SPRING_RABBITMQ_PASSWORD="${RMQ_PW}"  \
  $( docker images -aq $AN )
