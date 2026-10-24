package br.com.cotacao.web;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
public final class JsonConfig {
 private JsonConfig() {}
 public static ObjectMapper mapper() {
  return JsonMapper.builder().findAndAddModules()
   .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
   .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
   .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
   .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT).build();
 }
}
