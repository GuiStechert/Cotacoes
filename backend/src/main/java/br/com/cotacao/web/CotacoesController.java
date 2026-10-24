package br.com.cotacao.web;

import br.com.cotacao.application.CotacoesService;
import static br.com.cotacao.application.Cotacoes.*;
import java.time.LocalDate;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cotacoes")
public class CotacoesController {
 public record Criada(UUID id) {}
 private final CotacoesService service;
 public CotacoesController(CotacoesService service) {this.service=service;}
 @PostMapping public ResponseEntity<Criada> criar(@RequestBody Entrada entrada) { return criada(service.criar(entrada)); }
 @GetMapping public Pagina listar(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size,
     @RequestParam(required=false) String status, @RequestParam(required=false) UUID clienteId,
     @RequestParam(required=false) LocalDate inicio, @RequestParam(required=false) LocalDate fim) { return service.listar(new Filtro(page,size,status,clienteId,inicio,fim)); }
 @GetMapping("/{id}") public Visao consultar(@PathVariable UUID id) { return service.consultar(id); }
 @PutMapping("/{id}") public ResponseEntity<Void> editar(@PathVariable UUID id,@RequestBody Entrada entrada) { service.editar(id,entrada);return ResponseEntity.noContent().build(); }
 @PostMapping("/{id}/enviar") public ResponseEntity<Void> enviar(@PathVariable UUID id) { service.enviar(id);return ResponseEntity.noContent().build(); }
 @PostMapping("/{id}/duplicar") public ResponseEntity<Criada> duplicar(@PathVariable UUID id) { return criada(service.duplicar(id)); }
 private ResponseEntity<Criada> criada(UUID id) { return ResponseEntity.created(URI.create("/api/cotacoes/"+id)).body(new Criada(id)); }
}
