# Configuration 

We've seen that Spring can load configuration adjacent to the application `.jar`, and that it can load the configuration from environment variables and program arguments. It's not hard to get information into a Spring Boot application, but its sort of piecemeal. It's hard to version control environment variables or to secure program arguments. 

To solve some of these problems, the Spring Cloud team built the [Spring Cloud Config Server](https://cloud.spring.io/spring-cloud-config/multi/multi__spring_cloud_config_server.html). The Spring Cloud Config Server is an HTTP API that fronts a backend storage engine. The storage is pluggable, with the most common being a Git repository, though there is support for others as well. These include Subversion, a local file system, and even [MongoDB](https://github.com/spring-cloud-incubator/spring-cloud-config-server-mongodb). 

We're going to set up a new Spring Cloud Config Server. Go to the Spring Initializr, search for `Config Server`, and then click `Generate`. Open the resulting project in your favorite IDE.

We're going to need to do two things to make it work: first, we must use an annotation and then provide a configuration value to point it to the Git repository with our configuration file. Here, I'm using an open Github repository, but there's no reason you couldn't use any arbitrary Git repository and there's no reason that repository couldn't require authentication. All of these things are, you guessed it, _configurable_. 

Here is our `application.properties`:

```properties
spring.cloud.config.server.git.uri=https://github.com/joshlong/greetings-config-repository.git
server.port=8888
```

And here's what your main class should look like.

```java
package com.example.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@EnableConfigServer
@SpringBootApplication
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

Run the application - `mvn spring-boot:run` or just run the application in your favorite IDE. It'll act as a proxy to the Git configuration in the Github repository. Other clients can then use the Spring Cloud Config Client to pull their configuration in from the Spring Cloud Config Server, which will, in turn, pull it in from the Git repository. Please note, again, that I'm making this as insecure as possible for expediency of the demo, but you can (and should!) secure both links in the chain - from the config client to the config server, and from the config server to the Git repository. Spring Cloud Config Server, the Spring Cloud Config Client, and Github all work well together, and securely. 

Now we need to connect our `configuration` client to the service. Edit the `pom.xml` for the client and uncomment the Spring Cloud Config Client dependency.  To successfully connect to the Spring Cloud Config Server, our client will need to have some - you guessed it! - _configuration_ to tell our client where to find the Spring Cloud Config Server instance. A classic chicken and egg problem if ever there was one. This bootstrap configuration needs to be evaluated earlier, before the rest of the configuration for the application is loaded. You can put this configuration in a file called `src/main/resources/bootstrap.properties`. 

You'll need to identify your application - give it a name - so that the Spring Cloud Config Server will know which configuration to provide for us when it loads. The name we specify here will be matched to a property file in the Git repository. Here's what you should put in the file.

```properties
spring.cloud.config.uri=http://localhost:8888
spring.application.name=bootiful
```

When our client connects, it'll have access to everything in `application.properties` and everything in `bootiful.properties` in the Git repository. It'll even go one step further: if you run the client with a Spring profile active, that will let you load a configuration file with a profile, e.g., `bootiful-dev.properties`. 

Now we can read any value we want in the git repository in the `bootiful.properties` file. I've got the following in my `bootiful.properties`.

```
message-from-config-server = Hello, Spring Cloud Config Server
```

You can see what information the client will see by asking the Spring Cloud Config Server itself:

```shell
curl http://localhost:8888/bootiful/default
```

With all that background out of the way, we can inject any value we want just as before. Our Java code remains blissfully ignorant of the origin of the configuration.


```java
package com.example.configuration.configclient;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Log4j2
@SpringBootApplication
public class ConfigurationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigurationApplication.class, args);
    }

    @Bean
    ApplicationRunner applicationRunner(@Value("${message-from-config-server}") String configServer) {
        return args -> {
            log.info("message from the Spring Cloud Config Server: " + configServer);
        };
    }
}
```

You should see the value in the output. Not bad! The Spring Cloud Config Server does a lot of cool stuff for us. 

One interesting possibility that arises from this arrangement - having our configuration live external to our running JVM process in a mutable Spring Cloud Config Server - is that we can leverage Spring Cloud's `@RefreshScope` to dynamically reconfigure - to refactory them - beans that have changed via the Spring Boot Actuator. Uncomment the `Reactive Web` and `Actuator` support in the `pom.xml`. Add the following configuration to your client's `application.properties`: `management.endpoints.web.exposure.include=*`.

Here's the updated Java code demonstrating our application's ability to respond to external changes. 

```java
package com.example.configuration.refresh;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Log4j2
@SpringBootApplication
public class ConfigurationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigurationApplication.class, args);
    }
}


@Log4j2
@Component
@RefreshScope
class Refreshable {

    Refreshable(@Value("${message-from-config-server}") String msg) {
        log.info("value(s) in " + ConfigurationProperties.class.getName() + " bean is: " + msg);
    }
}

```

Confirm that when the application starts up, it logs out the configuration value from the config server as we expect it to. Then, change the value (it doesn't matter to what) in the origin Git repository and then run the following command to trigger an Actuator refresh endpoint and see that it recreates the `@RefreshScope`-annotated bean _in-situ_ and that you observe the updated value logged out:

```shell
$ curl localhost:8080/actuator/refresh -d {} -H "Content-Type: application/json"
```

You can take this a step further and generally make any component aware of the refresh event having been invoked. These beans may then reconfigure themselves (by reading in configuration and responding to it, for example).


Here's a `RefreshScopeRefreshedEvent` listener example: 

```java
@Log4j2
@Component
class RefreshListener {

    @EventListener
    public void refreshed(RefreshScopeRefreshedEvent rsre) {
        log.info("something has changed! " + rsre.getName());
    }
}

```

The Spring Cloud Config Server can encrypt values in the property files if you configure it appropriately. It works. A lot of folks also use Hashicorp's excellent Vault product, which is a much more fully-featured offering for security. Vault can secure, store, and tightly control access to tokens, passwords, certificates, encryption keys for protecting secrets, and other sensitive data using a UI, CLI, or HTTP API. You can also use this easily as a property source using the Spring Cloud Vault project. Uncomment the Sring Cloud Vault dependency from the build, and let us look at setting up Hashicorp Vault. 

Download the latest version and then run the following commands. I'm assuming a Linux or Unix-like environment. It should be fairly straightforward to translate to Windows, though. I won't try to explain everything about Vault; I'd refer you to the excellent Getting Statted guides for [Hashicorp Vault](https://learn.hashicorp.com/vault/getting-started/install), instead. Here's the least-secure, but quickest, the way I know to get this all set up and working. First, run the Vault server. I'm providing a root token here, but you would typically use the token provided by Vault on startup. 

```shell
export VAULT_ADDR="https://localhost:8200"
export VAULT_SKIP_VERIFY=true
export VAULT_TOKEN=00000000-0000-0000-0000-000000000000
vault server --dev --dev-root-token-id="00000000-0000-0000-0000-000000000000"
```

Once that's up, in another shell, install some values into the Vault server, like this.

```shell
export VAULT_ADDR="http://localhost:8200"
export VAULT_SKIP_VERIFY=true
export VAULT_TOKEN=00000000-0000-0000-0000-000000000000
vault kv put secret/bootiful message-from-vault-server="Hello Spring Cloud Vault"
```

That puts the key `message-from-vault-server` with a value `Hello Spring Cloud Vault` into the Vault service. Now, let's change our application to connect to that Vault instance to read the secure values. We'll need a bootstrap.properties, just as with the Spring Cloud Config Client. 

```properties
spring.application.name=bootiful
spring.cloud.vault.token=${VAULT_TOKEN}
spring.cloud.vault.scheme=http
```

Then, you can use the property just like any other configuration values. 

```java
package com.example.configuration.vault;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Log4j2
@SpringBootApplication
public class ConfigurationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigurationApplication.class, args);
    }

    @Bean
    ApplicationRunner applicationRunner(@Value("${message-from-vault-server:}") String valueFromVaultServer) {
        return args -> {
            log.info("message from the Spring Cloud Vault Server : " + valueFromVaultServer);
        };
    }
}
```

Now, before you run this, make sure also to have the same three environment variables we used in the two interactions with the `vault` CLI configured: `VAULT_TOKEN`, `VAULT_SKIP_VERIFY`, and `VAULT_ADDR`. Then run it, and you should see reflected on the console the value that you write to Hashicorp Vault. 

## Next Steps

Hopefully, you've learned something about the colorful and compelling world of configuration in SPring. With this information under your belt, you're now better prepared to use the other projects that support property resolution. Armed with this knowledge of how this works, you're ready to integrate configuration from different Spring integrations, of which there are a _ton_! You might use the Spring Cloud Netflix' Archaius integration, or the Configmaps integration with Spring Cloud Kubernetes, or the Spring Cloud GCP's Google Runtime Configuration API integration, or  Spring Cloud Azure's Microsoft Azure Key Vault integration, etc.

I've only mentioned a few offerings here, but it doesn't matter if the list is exhaustive, their use will be the same if the integration is correct: the cloud's the limit! 