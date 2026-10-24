package br.com.cotacao;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;

@SpringBootApplication(exclude={HibernateJpaAutoConfiguration.class,JpaRepositoriesAutoConfiguration.class,UserDetailsServiceAutoConfiguration.class})
public class CotacaoApplication {
 public static void main(String[] args) { SpringApplication.run(CotacaoApplication.class,args); }
}
