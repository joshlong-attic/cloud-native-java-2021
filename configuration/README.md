<iframe width="560" height="315" src="https://www.youtube.com/embed/PsNNGuLi0ns" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>


speaker: [Josh Long (@starbuxman) ](http://twitter.com/starbuxman)


Hi, Spring fans! Welcome to another installment of Spring tips! in this installment, we're going to look at something that's rather foundational, and something that I wish I'd addressed earlier: configuration. And no, I don't mean functional configuration or java configuration or anything like that, I'm talking about the string values that inform how your code executes. the stuff that you put in application.properites. _that_ configuration. 

All configuration in Spring emanates from the Spring `Environment` abstraction. The `Environment` is sort of like a dictionary - a map with keys and values. `Environment` is just an interface through which we can ask questions about, you know, the `Environment`. The abstraction lives in Spring Framework and was introduced in Spring 3, more than a decade ago. up until that point, there was a focused mechanism to allow integration of configuration called property placeholder resolution. This environment mechanism and the constellation of classes around that interface more than supersede that old support. if you find a blog still using those types, may I suggest you move on to newer and greener pastures? :) 

Let's get started. Go to the Spring Initializr and generate a new project and make sure to choose `Spring Cloud Vault`, `Spring Boot Configuration Processor`, `Actuator`, `Reactive Web`, `Lombok`, and  `Spring Cloud Config Client`. I named my project `configuration`. Go ahead and click `Generate` to generate the application. Open the project in your favorite IDE. If you want to follow along, be sure to disable the `Reactive Web`, `Actuator`, `Spring Cloud Vault` and `Spring Cloud Config Client` dependencies in your `pom.xml` by commenting them out. We don't need them right now.

The first step for most Spring Boot developers is to use `application.properties`. The Spring Initializr even puts an empty `application.properties` in the `src/main/resources/application.properties.` folder when you generate a new project! Super convenient. (You _do_ create your projects on the Spring Initializr, don't ya'?)  You could use `application.properties` or `application.yml`. I don't particularly love `.yml` files, but you can use 'em if  they're more your taste! 

Spring Boot automatically loads the `application.properties` whenever it starts up. You can dereference values from the property file in your java code through the environment. Put a property in the `application.properties` file, like this.

```properties
message-from-application-properties=Hello from application.properties
```

Now, let's write some code to read in that value using the Spring `Environment`. 

```java
package com.example.configuration;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@Log4j2
@SpringBootApplication
public class ConfigurationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigurationApplication.class, args);
    }

    @Bean
    ApplicationRunner applicationRunner(Environment environment) {
        return args -> {
            log.info("message from application.properties " + environment.getProperty("message-from-application-properties"));
        };
    }
}
```

Run this, and you'll see the value from the configuration property file in the output of the log. If you want to change which file Spring Boot reads by default, you can do that too. It's a chicken and egg problem, though - you need to specify a property that Spring Boot will use to figure out where to load all the properties. So you need to specify this outside of the `application.properties` file. You can use a program argument or an environment variable to fill the `spring.config.name` property. 

```shell 
export SPRING_CONFIG_NAME=foo
```

Re-run the application now with that environment variable in scope, and it'll fail because it'll try to load `foo.properties`, not `application.properties`.

Incidentally, you could also run the application with the configuration that lives _outside_ the application, adjacent to the jar, like this. If you run the application like this, the values in the external `applicatin.properties` will override the values inside the `.jar`.

```shell
.
├── application.properties
└── configuration-0.0.1-SNAPSHOT.jar

0 directories, 2 files
```

Spring Boot is aware of Spring profiles, as well. Profiles are a mechanism that lets you tag objects and property files so that they can be selectively activated or deactivated at runtime. This is great if you want to have an environment-specific configuration. You can tag a Spring bean or a configuration file as belonging to a particular profile, and Spring will automatically load it for you when that profile is activated.

Profile names are, basically, arbitrary. Some profiles are magic - that Spring honors in a particular way. The most interesting of these is `default`, which is activated when no other profile is active. But generally, the names are up to you. I find it very useful to map my profiles to different environments: `dev`, `qa`, `staging`, `prod`, etc.

Let's say that there's a profile called `dev`. Spring Boot will automatically load `application-dev.properties`. It'll load that in addition to applicatin.properties. If there are any conflicts between values in the two files, then the more specific file - the one with the profile - wins. You could have a default value that applies absent a particular profile, and then provide specifics in the config for a profile. 

You can activate a given profile in several different ways, but the easiest is just to specify it on the command line. Or you could turn it on in your IDE's run configurations dialog box. IntelliJ and Spring Tool Suite both provide a place to specify the profile to sue when running the application. You can also set an env var, `SPRING_PROFILES_ACTIVE`, or specify an argument on the command line `--spring.profiles.active`. Either one accepts a comma-delimited list of profiles - you can activate more than one profile at a time. 

Le'ts try that out. Create a file called `application-dev.properties`. Put the following value in it.


```properties
message-from-application-properties=Hello from dev application.properties
```

This property has the same key like the one in `application.properties`. The Java code here is identical to what we had before. Just be sure to specify the profile before you start the Spring application. You can use the environment variable (`export SPRING_PROFILES_ACTIVE=dev`), properties (`java -jar ... --spring.profiles.active=dev`), etc. You can even define it programmatically when building the `SpringApplication` in the `main()` method, which is what we do here. 

```java
package com.example.configuration.profiles;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@Log4j2
@SpringBootApplication
public class ConfigurationApplication {

    public static void main(String[] args) {
        // this works
        // export SPRING_PROFILES_ACTIVE=dev  
        // System.setProperty("spring.profiles.active", "dev"); // so does this
        new SpringApplicationBuilder()
            .profiles("dev") // and so does this
            .sources(ConfigurationApplication.class)
            .run(args);
    }

    @Bean
    ApplicationRunner applicationRunner(Environment environment) {
        return args -> {
            log.info("message from application.properties " + environment.getProperty("message-from-application-properties"));
        };
    }
}
```

Run the application, and you'll see the specialized message reflected in the output. 

So far, we've been using the `Environment` to lookup the configuration. You can also use SPring's `@Value` annotation to inject the value as a parameter in bean provider methods. (You probably already know that.) But did you know that you can also specify default values to be returned if there are no other values that match? There are a lot of reasons why you might want to do this. You could use it to provide fallback values and make it more transparent when somebody fat fingers the spelling of a property. It is also useful because you are given a value that might be useful if somebody doesn't know that they need to activate a profile or something,.


```java
package com.example.configuration.value;

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
    ApplicationRunner applicationRunner(
        @Value("${message-from-application-properties:OOPS!}") String valueDoesExist,
        @Value("${mesage-from-application-properties:OOPS!}") String valueDoesNotExist) {
        return args -> {
            log.info("message from application.properties " + valueDoesExist);
            log.info("missing message from application.properties " + valueDoesNotExist);
        };
    }
}
```

Convenient, eh? Also, note that the default String that you provide can, in turn, interpolate some other property. So you could do something like this, assuming a key like `default-error-message` does exist somewhere in your application configuration: 

```properties
${message-from-application-properties:${default-error-message:YIKES!}}
```

That will evaluate the first property if it exists, then the second, and then the `String` `YIKES!`, finally. 

Earlier, we looked at how to specify a profile using an environment variable or program argument. This mechanism - configuring Spring Boot with environment variables or program arguments - is a general-purpose. You can use it for any arbitrary key, and Spring Boot will normalize the configuration for you. Any key that you would put in `application.properties` can be specified externally in this way. Let's see some examples. Let's suppose you want to specify the URL for a database connection. You _could_ hardcode that value in the `application.properties`, but that's not very secure. It might be much better to create instead an environment variable that only exists in production. That way, the developers don't have access to the keys to the production database and so on. This is a sort of a tiered approach: you hardcode a sensible value for development in the property file, but then override that value using an environment variable in production. 

Let's try it out. Heres the Java code. 

```java

package com.example.configuration.envvars;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@Log4j2
@SpringBootApplication
public class ConfigurationApplication {

    public static void main(String[] args) {
        // simulate program arguments
        // you could also use an environment variable
        String[] actualArgs = new String[]{"spring.datasource.url=jdbc:postgres://localhost/some-prod-db"};
        SpringApplication.run(ConfigurationApplication.class, actualArgs);
    }

    @Bean
    ApplicationRunner applicationRunner(Environment environment) {
        return args -> {
            log.info("our database URL connection will be " + environment.getProperty("spring.datasource.url"));
        };
    }
}

```

Before you run it, be sure to either export an environment variable in the shell that you use to run your application or to specify a program argument.  I simulate the latter - the program arguments - by passing in arguments in the arguments array passed into `public static void main(String [] args)`. You can alternativley specify an environment variable like this: 

```shell
export SPRING_DATASOURCE_URL=some-arbitrary-value
mvn -DskipTests=true spring-boot:run 
```

Run the program multiple times, trying out the different approaches, and you will see the values in the output. There's no autoconfiguration in the application that will connect to a database, so we're using this property as an example. The URL doesn't have to be a valid URL (at least not until you add Spring's JDBC support and a JDBC driver to the classpath).

Spring Boot is _very_ flexible in its sourcing of the values. It doesn't care if you do `SPRING_DATASOURCE_URL`, `spring.datasource.url`, etc. Spring Boot calls this _relaxed binding_. It allows you to do things in a way that's most natural for different environments, while still working for Spring Boot.

This idea - of externalizing configuration for an application from the environment - is not new. It's well understood and described in the [12-factor manifesto](https://12factor.net/config). The 12-factor manifesto says that environment-specific config should live in that environment, not in the code itself. This is because we want one build for all the environments. Things that change should be external. So far, we've seen that Spring Boot can pull in configuration from the command line arguments (program arguments), and environment variables. It can also read configuration coming from [JOpt](https://jopt-simple.github.io/jopt-simple/). It can come even from a JNDI context if you happen to be running in an application server with one of those around! 

Spring Boots's ability to pull in any environment variable is beneficial here. Environment variables are also more secure, a better fit, than using program arguments because the program arguments will show up in the output of operating system tools like `history`. 

So far, we've seen that Spring Boot can pull in configuration from a lot of different places. It knows about profiles, it knows about `.yml.` and `.properties`.  It's pretty flexible! But what if it doesn't know how to do what you want it to do? You can easily reach its new tricks using a custom `PropertySource<T>`. You might want to do something like this to integrate your application with the configuration you're storing in an external database or a directory or some other things about which Spring Boot doesn't automatically know, but that you wish it did.

```java

package com.example.configuration.propertysource;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.PropertySource;

@Log4j2
@SpringBootApplication
public class ConfigurationApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder()
            .sources(ConfigurationApplication.class)
            .initializers(context -> context
                .getEnvironment()
                .getPropertySources()
                .addLast(new BootifulPropertySource())
            )
            .run(args);
    }

    @Bean
    ApplicationRunner applicationRunner(@Value("${bootiful-message}") String bootifulMessage) {
        return args -> {
            log.info("message from custom PropertySource: " + bootifulMessage);
        };
    }
}

class BootifulPropertySource extends PropertySource<String> {

    BootifulPropertySource() {
        super("bootiful");
    }

    @Override
    public Object getProperty(String name) {

        if (name.equalsIgnoreCase("bootiful-message")) {
            return "Hello from " + BootifulPropertySource.class.getSimpleName() + "!";
        }

        return null;
    }
}


```

The example above is the safest way to register a `PropertySource` early enough on that everything that needs it will be able to find it. You can also do it at runtime, when Spring has started wiring objects together, and you have access to configured objects, but I wouldn't be sure that this will work in every situation. Here's how that might look. 


```java
package com.example.configuration.propertysource;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

@Log4j2
@SpringBootApplication
public class ConfigurationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigurationApplication.class, args);
    }

    @Bean
    ApplicationRunner applicationRunner(@Value("${bootiful-message}") String bootifulMessage) {
        return args -> {
            log.info("message from custom PropertySource: " + bootifulMessage);
        };
    }

    @Autowired
    void contributeToTheEnvironment(ConfigurableEnvironment environment) {
        environment.getPropertySources().addLast(new BootifulPropertySource());
    }
}

class BootifulPropertySource extends PropertySource<String> {

    BootifulPropertySource() {
        super("bootiful");
    }

    @Override
    public Object getProperty(String name) {

        if (name.equalsIgnoreCase("bootiful-message")) {
            return "Hello from " + BootifulPropertySource.class.getSimpleName() + "!";
        }

        return null;
    }
}
```

The ideal solution for this, especially in a Spring Boot context, is to use an `EnvironmentPostProcessor`. This positions you early enough in the lifecycle to matter _and_ gives you access to the Spring Boot `SpringApplication` instance. Let's look at an example. 


```java
package com.example.configuration.environmentpostprocessor;


import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

@Log4j2
@SpringBootApplication
public class ConfigurationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigurationApplication.class, args);
    }

    @Bean
    ApplicationRunner applicationRunner(Environment environment) {
        return args -> log.info("message from " + BootifulEnvironmentProcessor.class.getName() + ": " + environment.getProperty("bootiful-message"));
    }
}


@Log4j2
class BootifulEnvironmentProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        log.info("contributing " + this.getClass().getName() + '.');
        environment.getPropertySources().addFirst(new BootifulPropertySource());
    }
}

class BootifulPropertySource extends PropertySource<String> {


    BootifulPropertySource() {
        super("bootiful");
    }

    @Override
    public Object getProperty(String name) {

        if (name.equalsIgnoreCase("bootiful-message")) {
            return "Hello from " + this.getClass().getSimpleName() + "!";
        }

        return null;
    }

}

```

Spring Boot won't know about this class unless you tell it about it. Create a file, `src/main/resources/META-INF/spring.factories`, and add the following entry:

```properties
org.springframework.boot.env.EnvironmentPostProcessor=com.example.configuration.environmentpostprocessor.BootifulEnvironmentProcessor
```

Start the application and you should see the logging appear confirming that Spring Boot is aware of the new configuration! 

Thus far, we've looked almost entirely at how to source property values from elsewhere. Still, we haven't talked about what becomes of the Strings once they're in our working memory and available for use in the application. Most of the time, they're just strings, and we can use them as-is. Sometimes, however, it's useful to turn them into other types of values - `int`s, `Date`s, `double`s, etc. This work - turning `String`s into things - could be the topic of a whole other [_Spring Tips_](http://bit.ly/spring-tips-playlist) video and perhaps one I'll do soon. Suffice it to say that there are a lot of interrelated pieces there - the `ConversionService`, `Converter<T>`s, Spring Boot's `Binder`s, and so much more. For common cases, this will just work. You can, for example, specify a property `server.port = 8080` and then inject it into your application as an `int`:

```java
@Value("${server.port}") int port
```

It might be helpful to have these values bound to an object automatically based on convention. Spring Boot has a ton of properties that leverage this very mechanism to make providing auto-configuration easier. We, the users of Spring Boot, provide a few properties, those get bound up into objects that the pre-provided Spring Boot autoconfiguration code then injects and dereferences to figure out how to do what we want it to do. But the mechanism is open to everyone. You can even get that nifty auto-completion for properties in your `application.properties` or `application.yml` in your favorite IDE, be it IntelliJ Ultimate, Eclipse, Netbeans, VS Code, etc., if you add the `Spring Boot Configuration Processor` to the build. This generates an index that all the aforementioned tools know how to read in to drive their autocompletion. Try it out. Add the following dependency.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

In this example we'll define a `@ConfigurationProperties`-annotatd object with a property prefix, `bootifulcp`, and then specify some configuration values. Here's the relevant Java code.

```java

package com.example.configuration.cp;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@Log4j2
@SpringBootApplication
@EnableConfigurationProperties(BootifulProperties.class)
public class ConfigurationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigurationApplication.class, args);
    }

    @Bean
    ApplicationRunner applicationRunner(BootifulProperties bootifulProperties) {
        return args -> {
            log.info("message from @ConfigurationProperties bootifulcp.message: " + bootifulProperties.getMessage());
            log.info("message from @ConfigurationProperties bootifulcp.favorite-number: " + bootifulProperties.getFavoriteNumber());
        };
    }

}

@Data
@RequiredArgsConstructor
@ConfigurationProperties("bootifulcp")
class BootifulProperties {
    private int favoriteNumber;
    private String message;
}
```

At this point I usually rebuild the project. Worst case, you might need to do `mvn clean package` on the command line and then reimport the project into your IDE so that the IDE sees the updated index generated by the `Spring Boot Configuration Processor`.

Now let's define configuration values for the properties in `application.properties` (`bootifulcp.message`, `bootifulcp.favorite-number`) and we'll see those bound to the object.

```property
bootiful.message = Hello from a @ConfiguratinoProperties 
bootiful.favorite-number = 42
```

You can run the application and see that the configuration value has been bound to the object for us.

This `@ConfigurationProperties` class uses JavaBean-style properties. You can  use `@ConstructorBinding`, in addition to the existing properties, if you prefer constructor-centric binding instead of mutable properties ("setters").

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