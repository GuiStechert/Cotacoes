package br.com.cotacao.web;

import br.com.cotacao.application.CotacoesService;
import br.com.cotacao.application.Cotacoes.Pagina;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/aprovacoes")
public class AprovacoesController {
 private final CotacoesService service;
 public AprovacoesController(CotacoesService service) {this.service=service;}
 @GetMapping("/pendentes") public Pagina pendentes(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.pendentes(page,size); }
}
