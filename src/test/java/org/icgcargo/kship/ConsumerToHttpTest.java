package org.icgcargo.kship;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
public class ConsumerToHttpTest {
  @Autowired
  private Sink sink;
  @Autowired
  private ConsumerToHttp shipper;

  @Test
  @SneakyThrows
  public void shouldShipToHttpServer() throws JsonProcessingException {
    val contentJsonNode = new ObjectMapper().readTree("{\"key\": \"value\"}");
    stubFor(
      request("POST", urlEqualTo("/target"))
        .willReturn(aResponse()
          .withBody("{ \"status\": \"ok\"}")
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
        )
    );
    this.sink.input().send(MessageBuilder.withPayload(contentJsonNode).build());
    // the send is async so we have to wait a bit for wiremock to respond
    Thread.sleep(1000);
    verify(postRequestedFor(urlMatching("/target")));
  }
}
