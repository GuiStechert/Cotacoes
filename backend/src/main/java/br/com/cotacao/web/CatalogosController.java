package br.com.cotacao.web;
import br.com.cotacao.application.CatalogoService;
import static br.com.cotacao.application.Catalogos.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogosController {
 private final CatalogoService service;
 public CatalogosController(CatalogoService service) {this.service=service;}
 @GetMapping("/clientes") public List<Cliente> clientes(){return service.clientes();}
 @GetMapping("/produtos") public List<? extends Produto> produtos(){return service.produtos();}
 @GetMapping("/usuario") public UsuarioAtual usuario(){return service.usuarioAtual();}
}
