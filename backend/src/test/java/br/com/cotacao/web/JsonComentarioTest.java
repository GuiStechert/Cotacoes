package br.com.cotacao.web;

import br.com.cotacao.application.Cotacoes.Decisoes;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JsonComentarioTest {
 @Test void comentarioDeveSerTextoSemCoercaoDeNumeroOuBooleano() {
  for(var valor:List.of("123","1.25","true")) {
   var json="{\"itens\":[{\"itemId\":\"00000000-0000-0000-0000-000000000001\",\"aprovado\":false,\"comentario\":"+valor+"}]}";
   assertThrows(MismatchedInputException.class,()->JsonConfig.mapper().readValue(json,Decisoes.class));
  }
 }
}
