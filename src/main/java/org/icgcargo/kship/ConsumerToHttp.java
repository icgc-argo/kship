package org.icgcargo.kship;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@ConditionalOnProperty("kship.http.url")
@EnableBinding(Sink.class)
public class ConsumerToHttp implements Shipper {

  private WebClient client;

  public ConsumerToHttp(@Value("${kship.http.url}") String targetUrl,
                        @Value("${kship.security.clientId}") String clientId,
                        @Value("${kship.security.clientSecret}") String clientSecret) {
    val wcb = WebClient.builder()
        .baseUrl(targetUrl);

    if (!StringUtils.isEmpty(clientId) && !StringUtils.isEmpty(clientSecret)) {
      String credentials = clientId + ":" + clientSecret;
      wcb.defaultHeader("Authorization", "Basic " + new String(Base64Utils.encode(credentials.getBytes())));
    }
    this.client = wcb.build();
    ;
  }

  @StreamListener(Sink.INPUT)
  public void ship(@Payload JsonNode message) {
    log.debug("received a message : " + message.toPrettyString());
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
  }
}