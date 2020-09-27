package com.example.locks;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.integration.support.leader.LockRegistryLeaderInitiator;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Repository;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.*;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
public class LocksApplication {

    public static void main(String[] args) {
        SpringApplication.run(LocksApplication.class, args);
    }

    @Bean
    DefaultLockRepository defaultLockRepository(DataSource dataSource) {
        return new DefaultLockRepository(dataSource);
    }

    @Bean
    JdbcLockRegistry jdbcLockRegistry(LockRepository repository) {
        return new JdbcLockRegistry(repository);
    }

    @Bean
    LockRegistryLeaderInitiator leaderInitiator(LockRegistry lockRegistry) {
        return new LockRegistryLeaderInitiator(lockRegistry);
    }
}

@Log4j2
@Repository
class FileRepository {

    private final File file;

    FileRepository(@Value("${file-repository.file:file://${user.home}/Desktop/synchronized-file}") File file) {
        this.file = file;
        log.info("going to open " + this.file.getAbsolutePath() + '.');
    }

    String read() throws Exception {
        try (var r = new BufferedReader(
                new FileReader(this.file))) {
            return FileCopyUtils.copyToString(r);
        }
    }

    void update(String msg) throws Exception {
        try (var w = new BufferedWriter(new FileWriter(this.file))) {
            w.write(msg);
        }
    }
}

@Log4j2
@RestController
@RequiredArgsConstructor
class FileRestController {

    private final FileRepository fileRepository;
    private final LockRegistry registry;

    private final String key = FileRepository.class.getName();

    private final AtomicInteger port = new AtomicInteger();

    @EventListener
    public void webServerInitialized(WebServerInitializedEvent se) {
        this.port.set(se.getWebServer().getPort());
    }

    /**
     * usage: {@code curl http://localhost:PORT1/A/10} and then wait six seconds. Then {@code  http://localhost:PORT2/B/10}. If you run
     * {@code watch -n1 cat ${user.home}/Desktop/synchronized-file} then you'll see it'll be updated to the first writer and then to the second writer.
     *
     * Now repeat the process, except this time curl PORT1 and only wait one or two seconds then curl the second one.
     * You'll see the second one will give up after five seconds (which is still less than the 10
     * seconds the first is waiting before relinquishing the lock) and so won't be able to effect a write.
     */
    @GetMapping("/{name}/{wait}")
    String update(@PathVariable String name, @PathVariable Integer wait) throws Exception {
        var lock = registry.obtain(this.key);
        try {
            log.info("attempting to acquire lock....");
            if (lock.tryLock(5, TimeUnit.SECONDS)) {
                log.info("acquired lock....");
                this.fileRepository.update("Hello " + name + " @ " + Instant.now() + " from " + this.port.get() + "!");
                log.info("sleeping " + wait + "s");
                Thread.sleep(wait * 1000);
                log.info("slept " + wait + "s");
            }
        } finally {
            lock.unlock();
        }

        return this.fileRepository.read();
    }

}
