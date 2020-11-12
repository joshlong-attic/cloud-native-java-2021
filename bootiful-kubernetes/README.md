Spring Boot project with Spring WebFlux and Spring Boot actuators.

The `application.properties` is:

```
spring.main.cloud-platform=kubernetes
management.endpoint.shutdown.enabled=true
management.endpoints.web.exposure.include=shutdown,health,info
```

# Building the app

To build the application there are options:

## Build with the compile script

```
./compile.sh
```

## Build with maven (no script)

```
mvn -Pnative clean package
```

## Build with Spring container image support using buildpacks

```
mvn spring-boot:build-image
```

# Launching the app

If building without container image support the executable is in the target folder:

```
./target/webflux-actuator-graalvm
```

If building as a container image the `docker-compose.yml` can be used to launch it:

```
docker-compose up webflux-actuator-graal
```


To build and run the native application packaged in a lightweight container:
```
./gradlew bootBuildImage
docker-compose up
```

# Does it start fast?

```
docker-compose up

Starting webflux-actuator-graal_actuator-webflux_1 ... done
Attaching to webflux-actuator-graal_actuator-webflux_1
actuator-webflux_1  |
actuator-webflux_1  |   .   ____          _            __ _ _
actuator-webflux_1  |  /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
actuator-webflux_1  | ( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
actuator-webflux_1  |  \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
actuator-webflux_1  |   '  |____| .__|_| |_|_| |_\__, | / / / /
actuator-webflux_1  |  =========|_|==============|___/=/_/_/_/
actuator-webflux_1  |  :: Spring Boot ::
actuator-webflux_1  |
actuator-webflux_1  | 2020-10-06 02:43:48.091  INFO 1 --- [           main] c.example.actuator.ActuatorApplication   : Starting ActuatorApplication using Java 1.8.0_262 on 813cbae726bb with PID 1 (/workspace/com.example.actuator.ActuatorApplication started by cnb in /workspace)
actuator-webflux_1  | 2020-10-06 02:43:48.091  INFO 1 --- [           main] c.example.actuator.ActuatorApplication   : No active profile set, falling back to default profiles: default
actuator-webflux_1  | 2020-10-06 02:43:48.156  INFO 1 --- [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 3 endpoint(s) beneath base path '/actuator'
actuator-webflux_1  | 2020-10-06 02:43:48.162  WARN 1 --- [           main] i.m.c.i.binder.jvm.JvmGcMetrics          : GC notifications will not be available because MemoryPoolMXBeans are not provided by the JVM
actuator-webflux_1  | 2020-10-06 02:43:48.173  WARN 1 --- [           main] io.netty.channel.DefaultChannelId        : Failed to find the current process ID from ''; using a random value: 923347415
actuator-webflux_1  | 2020-10-06 02:43:48.175  INFO 1 --- [           main] o.s.b.web.embedded.netty.NettyWebServer  : Netty started on port(s): 8080
actuator-webflux_1  | 2020-10-06 02:43:48.176  INFO 1 --- [           main] c.example.actuator.ActuatorApplication   : Started ActuatorApplication in 0.094 seconds (JVM running for 0.097)
```

Startup is < 100ms.


# Testing it out

```
curl http://localhost:8080
Yo!

curl http://localhost:8080/actuator/health
{"groups":["liveness","readiness"],"status":"UP"}

curl localhost:8080/actuator/health/readiness
{"status":"UP"}

curl localhost:8080/actuator/health/liveness
{"status":"UP"}

curl -X POST http://localhost:8080/actuator/shutdown
{"message":"Shutting down, bye..."}
```
