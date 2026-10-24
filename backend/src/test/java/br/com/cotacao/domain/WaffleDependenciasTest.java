package br.com.cotacao.domain;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WaffleDependenciasTest {
 @Test void waffleNaoRemoveAPonteDeLoggingNecessariaAoSpring() {
  assertDoesNotThrow(()->Class.forName("org.apache.commons.logging.LogFactory"));
  assertDoesNotThrow(()->Class.forName("waffle.spring.NegotiateSecurityFilter"));
 }
}
