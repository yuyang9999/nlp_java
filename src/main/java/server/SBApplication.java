package server;

import article.summary.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by yuyang on 13/9/17.
 */

@SpringBootApplication
public class SBApplication {
    public static void main(String[] args) throws Exception {
        SpringApplication.run(SBApplication.class, args);
    }
}
