#!/usr/bin/env bash 

export APP_NAME=bootiful-k8s
export PROJECT_ID=bootiful

IMAGE_NAME=gcr.io/${PROJECT_ID}/${APP_NAME}

./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=${IMAGE_NAME}

docker push ${IMAGE_NAME}

kubectl create deployment ${APP_NAME} --image=${IMAGE_NAME}
kubectl expose deployment ${APP_NAME} --port=80 --target-port=8080 --name=${APP_NAME} --type=LoadBalancer

kubectl describe services $APP_NAME
