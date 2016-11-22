package com.uken.platform.leaderboard.verticles;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;

@Component
public class LeaderboardWorker extends AbstractVerticle {

	private static final String LEADERBOARD_NAME = "leaderboard";
	
	@Autowired
	private RedisClient redisClient;

	/**
	 * Entry point for this {@link io.vertx.core.Verticle}
	 * 
	 * @throws Exception
	 */
	@Override
	public void start() throws Exception {
		// Register a consumer on the event bus to listen for message sent to
		// "com.zanclus.customer"
		vertx.eventBus().consumer("com.uken.platform").handler(this::handleRedisRequest);
	}

	/**
	 * Handle database operations for a given message
	 * 
	 * @param msg
	 *            The {@link Message} containing the information to tell which
	 *            database operations to perform
	 */
	public void handleRedisRequest(Message<Object> msg) {
		String method = msg.headers().get("method");

		DeliveryOptions opts = new DeliveryOptions();
		JsonObject body;
		String user;

		switch (method) {

		case "setScore":
			body = (JsonObject) msg.body();
			redisClient.zadd(LEADERBOARD_NAME, body.getDouble("score"), body.getString("user"), r ->{
				if(r.succeeded()){
					msg.reply(true);
				} else {
					System.out.println("Redis operation failed" + r.cause());
				}
			});
			
			break;
		case "getScore":
			user = (String) msg.body();
			redisClient.zscore(LEADERBOARD_NAME, user, r ->{
				if(r.succeeded()){
					msg.reply(r.result());
				} else {
					System.out.println("Redis operation failed" + r.cause());
				}
			});
			break;
		case "incrementScore":
			body = (JsonObject) msg.body();
			redisClient.zincrby(LEADERBOARD_NAME, body.getDouble("delta"), body.getString("user"), r ->{
				if(r.succeeded()){
					msg.reply(r.result());
				} else {
					System.out.println("Redis operation failed" + r.cause());
				}
			});

			break;
		case "decrementScore":
			body = (JsonObject) msg.body();
			redisClient.zincrby(LEADERBOARD_NAME, -body.getDouble("delta"), body.getString("user"), r ->{
				if(r.succeeded()){
					msg.reply("OK");
				} else {
					System.out.println("Redis operation failed" + r.cause());
				}
			});
			break;
		case "getRank":
			user = (String) msg.body();
			redisClient.zrevrank(LEADERBOARD_NAME, user, r ->{
				if(r.succeeded()){
					msg.reply(r.result());
				} else {
					System.out.println("Redis operation failed" + r.cause());
				}
			});
			break;
		case "getUsersByRankRange":
			String[] params = ((String) msg.body()).split(Pattern.quote("."));
			long bottom = Long.parseLong(params[0]);
			long top = Long.parseLong(params[1]);
			
			redisClient.zrevrange(LEADERBOARD_NAME, bottom, top, null, r ->{
				if(r.succeeded()){
					msg.reply(r.result());
				} else {
					System.out.println("Redis operation failed" + r.cause());
				}
			});
			break;
		default:
			System.err.println("Invalid method '" + method + "'");
			opts.addHeader("error", "Invalid method '" + method + "'");
			msg.fail(1, "Invalid method");
		}
	}



}
