# Cloud Native Java 2021

Cloud Native Java, 2021 Redux. This contains the code to the  updated version of my talk "Cloud Native Java"  



## Bootiful Kubernetes 

* 	Build a Spring Boot application that uses Packeto and Buildacks and publishes it to a registry 
* 	Here's how you publish it to a registry like [Google's Container Registry](http://gcr.io):  
```

export APP_NAME=hello-java
export PROJECT_ID=bootiful

mvn -DskipTests=true clean spring-boot:build-image
image_id=$(docker images -q $APP_NAME)

docker tag "${image_id}" gcr.io/${PROJECT_ID}/${APP_NAME}
docker push gcr.io/${PROJECT_ID}/${APP_NAME}
docker pull gcr.io/${PROJECT_ID}/${APP_NAME}:latest

kubectl create deployment ${APP_NAME} --image=gcr.io/${PROJECT_ID}/${APP_NAME}
kubectl expose deployment ${APP_NAME} --port=80 --target-port=8080 --name=${APP_NAME} --type=LoadBalancer

kubectl describe services $APP_NAME

```