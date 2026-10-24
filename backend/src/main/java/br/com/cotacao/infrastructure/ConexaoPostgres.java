package br.com.cotacao.infrastructure;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
public final class ConexaoPostgres {
 private ConexaoPostgres() {}
 private static final Pattern URI=Pattern.compile("^postgres(?:ql)?://([^:/?#]+):(.*)@(\\[[^]]+\\]|[^/:?#\\s]+)(?::(\\d+))?/([^/?#\\s]+)(?:\\?([^#\\s]*))?$");
 public static DriverManagerDataSource criar(String url) {
  try {
   if(url==null)throw new IllegalArgumentException();
   var m=URI.matcher(url);if(!m.matches())throw new IllegalArgumentException();
   int porta=m.group(4)==null?5432:Integer.parseInt(m.group(4));
   if(porta<1||porta>65535)throw new IllegalArgumentException();
   String query=m.group(6)==null?"":m.group(6);
   if(Pattern.compile("(?i)(^|&)(user|password)=").matcher(query).find())throw new IllegalArgumentException();
   if(!Pattern.compile("(?i)(^|&)currentSchema=").matcher(query).find())query+=(query.isEmpty()?"":"&")+"currentSchema=cotacao";
   var ds=new DriverManagerDataSource();
   ds.setUrl("jdbc:postgresql://"+m.group(3)+":"+porta+"/"+m.group(5)+"?"+query);
   ds.setUsername(decodificar(m.group(1)));ds.setPassword(decodificar(m.group(2)));
   return ds;
  } catch(RuntimeException erro) { throw new IllegalArgumentException("DATABASE_URL inválida"); }
 }
 private static String decodificar(String valor) { return URLDecoder.decode(valor.replace("+","%2B"),StandardCharsets.UTF_8); }
}
