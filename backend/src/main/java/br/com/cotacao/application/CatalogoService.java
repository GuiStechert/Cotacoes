package br.com.cotacao.application;
import br.com.cotacao.infrastructure.CatalogoRepository;
import org.springframework.transaction.support.TransactionOperations;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import br.com.cotacao.application.Cotacoes.Erro;
import static br.com.cotacao.application.Catalogos.*;

public class CatalogoService {
 private final CatalogoRepository repository;
 private final TransactionOperations tx;
 private final Identidade identidade;
 public CatalogoService(CatalogoRepository repository,TransactionOperations tx,Identidade identidade) {
  this.repository=Objects.requireNonNull(repository);this.tx=Objects.requireNonNull(tx);this.identidade=Objects.requireNonNull(identidade);
 }
 public List<Cliente> clientes(){return tx.execute(t->{exigirUsuario();return repository.clientes();});}
 public List<? extends Produto> produtos(){return tx.execute(t->{var usuario=exigirUsuario();return usuario.perfil().equals("COMERCIAL")?repository.produtosComerciais():repository.produtosAprovadores();});}
 public UsuarioAtual usuarioAtual(){return tx.execute(t->exigirUsuario());}
 private UsuarioAtual exigirUsuario(){
  var conta=identidade.contaAutenticada();if(conta==null||conta.isBlank())throw new Erro("NAO_AUTENTICADO");
  var usuario=repository.usuario(conta);
  if(usuario==null||!Set.of("COMERCIAL","GESTOR","SUPERVISOR","GERENTE").contains(usuario.perfil()))throw new Erro("ACESSO_NEGADO");
  return usuario;
 }
}
