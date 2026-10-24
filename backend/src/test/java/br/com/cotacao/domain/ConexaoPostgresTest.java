package br.com.cotacao.domain;
import br.com.cotacao.infrastructure.ConexaoPostgres;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
class ConexaoPostgresTest {
 @Test void converteUriParaJdbcComSchemaDoProjeto(){var ds=ConexaoPostgres.criar("postgresql://usuario:segredo@localhost:5432/banco");assertEquals("jdbc:postgresql://localhost:5432/banco?currentSchema=cotacao",ds.getUrl());assertEquals("usuario",ds.getUsername());assertEquals("segredo",ds.getPassword());}
 @Test void aceitaEscapesEArrobaLegadoSemVazarCredenciais(){var ds=ConexaoPostgres.criar("postgres://nome%2Bteste:senha@legada@localhost:5432/banco?sslmode=require");assertEquals("nome+teste",ds.getUsername());assertEquals("senha@legada",ds.getPassword());assertEquals("jdbc:postgresql://localhost:5432/banco?sslmode=require&currentSchema=cotacao",ds.getUrl());}
 @Test void schemaDeTesteExplicitoPreservado(){var ds=ConexaoPostgres.criar("postgresql://u:@127.0.0.1:55432/postgres?currentSchema=teste");assertEquals("jdbc:postgresql://127.0.0.1:55432/postgres?currentSchema=teste",ds.getUrl());}
 @Test void invalidaTemMensagemGenerica(){for(var url:Arrays.asList(null,"","http://u:segredo@host/db","postgresql://semcredencial/db","postgresql://u:segredo@host:abc/db")){var erro=assertThrows(IllegalArgumentException.class,()->ConexaoPostgres.criar(url));assertEquals("DATABASE_URL inválida",erro.getMessage());assertNull(erro.getCause());}}
}
