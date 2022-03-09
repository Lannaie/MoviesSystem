package com.self.showing;

import com.bonnie.movie_system.Kafka2HBase;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.text.ParseException;

@SpringBootApplication
public class ShowingApplication {

    public static void main(String[] args) throws IOException, ParseException {
        Kafka2HBase kh = new Kafka2HBase();
        kh.start_kafka2HBase();
        SpringApplication.run(ShowingApplication.class, args);
    }
}
