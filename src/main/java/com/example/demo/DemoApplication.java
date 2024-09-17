package com.example.demo;

import com.example.demo.handler.MyWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication

public class DemoApplication implements CommandLineRunner {


	private static final Logger log = LoggerFactory.getLogger(DemoApplication.class);

	@Autowired
	private MyWebSocketHandler myWebSocketHandler;

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
//		// Example: Broadcast a message every 5 seconds
		log.info("start");
		myWebSocketHandler.createDeck();
		myWebSocketHandler.shuffleDeck();

	}

}
