package br.com.cotacao.domain;
import br.com.cotacao.support.CenarioBanco;
import static br.com.cotacao.application.Cotacoes.*;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class QuantidadePrecisaoTest {
 private CenarioBanco c;
 @BeforeEach void preparar() throws Exception {c=new CenarioBanco();}
 @AfterEach void limpar(){if(c!=null)c.close();}
 private Entrada entrada(String quantidade){return new Entrada(c.cliente,LocalDate.of(2026,1,1),LocalDate.of(2026,8,1),BigDecimal.ZERO,null,List.of(new ItemEntrada(c.produto,new BigDecimal(quantidade),BigDecimal.TEN)));}
 @Test void quantidadeDigitadaComTresCasasEhRejeitadaSemArredondar(){assertEquals("DADOS_INVALIDOS",assertThrows(Erro.class,()->c.service.criar(entrada("1.231"))).codigo());assertEquals("0",c.valor("SELECT count(*) FROM cotacoes"));}
 @Test void quantidadeComDuasCasasOuZeroFinalEhValida(){assertNotNull(c.service.criar(entrada("1.23")));assertNotNull(c.service.criar(entrada("1.230")));}
}
