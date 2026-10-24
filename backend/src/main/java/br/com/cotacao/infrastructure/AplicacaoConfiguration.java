package br.com.cotacao.infrastructure;

import br.com.cotacao.application.*;
import br.com.cotacao.security.IdentidadeSpring;
import br.com.cotacao.web.JsonConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import javax.sql.DataSource;

@Configuration
public class AplicacaoConfiguration {
 @Bean public DataSource dataSource(Environment ambiente) { return ConexaoPostgres.criar(ambiente.getProperty("DATABASE_URL")); }
 @Bean public Clock clock() { return Clock.system(ZoneId.of("America/Cuiaba")); }
 @Bean public Identidade identidade() { return new IdentidadeSpring(); }
 @Bean @Primary public ObjectMapper objectMapper() { return JsonConfig.mapper(); }
 @Bean public CotacaoRepository repository(JdbcTemplate jdbc) { return new CotacaoRepository(jdbc); }
 @Bean public TransactionTemplate transacoes(DataSource ds) {
  var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
  tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
  return tx;
 }
 @Bean public CotacoesService cotacoesService(CotacaoRepository repo,TransactionTemplate tx,Identidade identidade,Clock clock) {
  return new CotacoesService(repo,tx,identidade,clock);
 }
}
