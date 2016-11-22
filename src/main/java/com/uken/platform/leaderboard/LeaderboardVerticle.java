package com.uken.platform.leaderboard;

import org.springframework.stereotype.Component;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

@Component
public class LeaderboardVerticle extends AbstractVerticle {

	@Override
	public void start() {

		System.out.println("Successfully create Verticle:" + this.getClass().getName());

		Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());

		final DeliveryOptions opts = new DeliveryOptions().setSendTimeout(2000);

		router.post("/score/set").consumes("application/json").handler(rc -> {
			opts.addHeader("method", "setScore");
			vertx.eventBus().send("com.uken.platform", rc.getBodyAsJson(), opts, reply -> handleReply(reply, rc));
		});

		router.get("/score/get/:user").handler(rc -> {
			opts.addHeader("method", "getScore");
			vertx.eventBus().send("com.uken.platform", rc.request().getParam("user"), opts,
					reply -> handleReply(reply, rc));
		});

		router.post("/score/increment").consumes("application/json").handler(rc -> {
			opts.addHeader("method", "incrementScore");
			vertx.eventBus().send("com.uken.platform", rc.getBodyAsJson(), opts, reply -> handleReply(reply, rc));
		});

		router.post("/score/decrement").consumes("application/json").handler(rc -> {
			opts.addHeader("method", "decrementScore");
			vertx.eventBus().send("com.uken.platform", rc.getBodyAsJson(), opts, reply -> handleReply(reply, rc));
		});

		router.get("/rank/get/:user").handler(rc -> {
			opts.addHeader("method", "getRank");
			vertx.eventBus().send("com.uken.platform", rc.request().getParam("user"), opts,
					reply -> handleReply(reply, rc));
		});

		router.get("/rank/get").produces("application/json").handler(rc -> {
			opts.addHeader("method", "getUsersByRankRange");
			String params = rc.request().getParam("bottom") + "." + rc.request().getParam("top");
			vertx.eventBus().send("com.uken.platform", params, opts, reply -> handleReply(reply, rc));
		});

		vertx.createHttpServer().requestHandler(router::accept).listen(8080);
	}

	/**
	 * Handle reply messages and convert them to
	 * {@link io.netty.handler.codec.http.HttpResponse} values.
	 * 
	 * @param reply
	 *            The reply message
	 * @param rc
	 *            The {@link RoutingContext}
	 */
	private void handleReply(AsyncResult<Message<Object>> reply, RoutingContext rc) {
		if (reply.succeeded()) {
			Message<Object> replyMsg = reply.result();
			if (reply.succeeded()) {
				rc.response().setStatusMessage("OK").setStatusCode(200).putHeader("Content-Type", "application/json")
						.end(replyMsg.body().toString());
			} else {
				rc.response().setStatusCode(500).setStatusMessage("Server Error")
						.end(reply.cause().getLocalizedMessage());
			}
		}
	}
}
