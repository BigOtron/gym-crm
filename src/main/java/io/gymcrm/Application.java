package io.gymcrm;

import io.gymcrm.config.AppConfig;
import io.gymcrm.web.config.WebConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.nio.file.Files;

@Slf4j
public class Application {

    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getProperty("server.port", String.valueOf(DEFAULT_PORT)));

        AnnotationConfigWebApplicationContext spring = new AnnotationConfigWebApplicationContext();
        spring.register(AppConfig.class, WebConfig.class);

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.setBaseDir(Files.createTempDirectory("gymcrm-tomcat").toString());
        tomcat.getConnector();

        StandardContext context = (StandardContext) tomcat.addContext("", null);
        context.setFailCtxIfServletStartFails(true);
        Tomcat.addServlet(context, "dispatcher", new DispatcherServlet(spring)).setLoadOnStartup(1);
        context.addServletMapping("/", "dispatcher");

        try {
            tomcat.start();
        } catch (LifecycleException e) {
            log.error("Gym CRM failed to start", e);
            tomcat.destroy();
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Stopping Gym CRM");
            try {
                tomcat.stop();
                tomcat.destroy();
            } catch (LifecycleException e) {
                log.warn("Error while stopping Tomcat", e);
            }
            spring.close();
        }));

        log.info("Gym CRM started on http://localhost:{}", port);
        tomcat.getServer().await();
    }
}