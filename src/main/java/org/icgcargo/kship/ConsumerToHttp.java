package org.icgcargo.kship;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j

@EnableBinding(Sink.class)
public class ConsumerToHttp implements Shipper {

  private WebClient client;

  public ConsumerToHttp(@Autowired WebClient webClient) {
    this.client = webClient;
  }

  @StreamListener(Sink.INPUT)
  public void ship(@Payload JsonNode message) {
    log.debug("received a message : " + message.toPrettyString());
    try {
      val res = client.post()
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(message)
          .retrieve()
          .bodyToMono(JsonNode.class)
          // we want this to block the main consumer thread so error handling works
          // as expected in cloud stream (we want to propagate errors up).
          .block();

      log.debug("shipped the message");
      if (res != null) {
        log.debug("response body was : " + res.toPrettyString());
      }
    } catch (Throwable e) {
      log.error("failed to send message", e);
      throw e;
    }
  }
}

