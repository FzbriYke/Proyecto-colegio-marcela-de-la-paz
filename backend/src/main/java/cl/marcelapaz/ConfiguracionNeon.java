package cl.marcelapaz;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("neon")
class ConfiguracionNeon {
    @Bean(destroyMethod = "close")
    HikariDataSource postgres(
        @Value("${spring.datasource.url}") String url,
        @Value("${spring.datasource.username}") String usuario,
        @Value("${spring.datasource.password}") String clave
    ) {
        var config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(usuario);
        config.setPassword(clave);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(0);
        var datos = new HikariDataSource(config);
        try {
            new JdbcTemplate(datos).execute("""
                CREATE TABLE IF NOT EXISTS escuela_colecciones (
                    nombre TEXT PRIMARY KEY,
                    contenido JSONB NOT NULL DEFAULT '[]'::jsonb
                )
                """);
            return datos;
        } catch (RuntimeException error) {
            datos.close();
            throw error;
        }
    }
}
