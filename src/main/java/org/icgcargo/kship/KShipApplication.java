package org.icgcargo.kship;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.UnAuthenticatedServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class KShipApplication {
  public static void main(String[] args) {
    SpringApplication.run(KShipApplication.class, args);
  }
}

@Configuration
@ConditionalOnProperty("kship.http.url")
class ConsumerToHttpConfig {

  // https://manhtai.github.io/posts/spring-webclient-oauth2-client-credentials/
  @Bean
  @ConditionalOnProperty(value = "kship.http.security.enabled", havingValue = "true")
  ReactiveClientRegistrationRepository getRegistration(
      @Value("${spring.security.oauth2.client.provider.ego.token-uri}") String tokenUri,
      @Value("${spring.security.oauth2.client.registration.ego-client.client-id}") String clientId,
      @Value("${spring.security.oauth2.client.registration.ego-client.client-secret}")
          String clientSecret) {
    ClientRegistration registration =
        ClientRegistration.withRegistrationId("ego-client")
            .tokenUri(tokenUri)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .build();
    return new InMemoryReactiveClientRegistrationRepository(registration);
  }

  @Bean()
  @ConditionalOnProperty(value = "kship.http.security.enabled", havingValue = "true")
  WebClient secureWebClient(
      ReactiveClientRegistrationRepository clientRegistrations,
      @Value("${kship.http.url}") String targetUrl) {

    ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
        new ServerOAuth2AuthorizedClientExchangeFilterFunction(
            clientRegistrations, new UnAuthenticatedServerOAuth2AuthorizedClientRepository());

    oauth.setDefaultClientRegistrationId("ego-client");

    return WebClient.builder().filter(oauth).baseUrl(targetUrl).build();
  }

  @Bean()
  @ConditionalOnProperty(value = "kship.http.security.enabled", havingValue = "false")
  WebClient insecureWebClient(@Value("${kship.http.url}") String targetUrl) {
    return WebClient.builder().baseUrl(targetUrl).build();
  }
}
