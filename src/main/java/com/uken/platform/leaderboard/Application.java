package com.uken.platform.leaderboard;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

import com.uken.platform.leaderboard.verticles.LeaderboardWorker;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

@SpringBootApplication
@PropertySource("application.properties")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Value("${redis.hostname}")
	private String redisHostName;

	@Value("${redis.port}")
	private int redisPort;

	@Value("${vertx.instances}")
	private int vertxInstances;

	private Vertx vertx;

	@Autowired
	private LeaderboardWorker leaderbaardWorker;

	@Bean
	public Vertx getVertxInstance() {
		if (this.vertx == null) {
			this.vertx = Vertx.vertx();
		}
		return this.vertx;
	}

	@Bean
	public RedisClient redisClient() {
		return RedisClient.create(getVertxInstance(), new RedisOptions().setHost(redisHostName).setPort(redisPort));
	}

	@PostConstruct
	public void deployVerticle() {
		getVertxInstance().deployVerticle("com.uken.platform.leaderboard.LeaderboardVerticle",
				new DeploymentOptions().setWorker(true).setMultiThreaded(false).setInstances(vertxInstances));
		getVertxInstance().deployVerticle(leaderbaardWorker,
				new DeploymentOptions().setWorker(true).setMultiThreaded(true));

	}

}
