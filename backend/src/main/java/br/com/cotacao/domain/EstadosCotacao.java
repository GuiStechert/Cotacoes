package br.com.cotacao.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class EstadosCotacao {
 private EstadosCotacao() {}
 public static String consolidar(Collection<String> estados) {
  if(estados==null||estados.isEmpty())throw invalido();
  var encontrados=new HashSet<String>();
  var permitidos=Set.of("RASCUNHO","PENDENTE","APROVADO","REPROVADO");
  for(var estado:estados){if(estado==null||!permitidos.contains(estado))throw invalido();encontrados.add(estado);}
  if(encontrados.contains("RASCUNHO")){
   if(encontrados.size()!=1)throw invalido();
   return "RASCUNHO";
  }
  if(encontrados.contains("PENDENTE"))return "PENDENTE_APROVACAO";
  if(encontrados.size()==2)return "APROVADA_PARCIAL";
  return encontrados.contains("APROVADO")?"APROVADA":"REPROVADA";
 }
 private static IllegalArgumentException invalido(){return new IllegalArgumentException("Estados de itens inválidos");}
}
