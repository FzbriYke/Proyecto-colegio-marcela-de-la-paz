package cl.marcelapaz;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

// Bloquea la colección mientras se valida y guarda una modificación.
final class ColeccionPostgres<T> implements ColeccionJson<T> {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaccion;
    private final String nombre;
    private final Class<T[]> tipo;
    private final ObjectMapper mapper;

    ColeccionPostgres(DataSource datos, String nombre, Class<T[]> tipo, ObjectMapper mapper) {
        this.jdbc = new JdbcTemplate(datos);
        this.transaccion = new TransactionTemplate(new DataSourceTransactionManager(datos));
        this.nombre = nombre;
        this.tipo = tipo;
        this.mapper = mapper;
        jdbc.update("INSERT INTO escuela_colecciones (nombre) VALUES (?) ON CONFLICT (nombre) DO NOTHING", nombre);
    }

    @Override
    public List<T> leer() {
        return leer(false);
    }

    private List<T> leer(boolean bloquear) {
        var sql = "SELECT contenido::text FROM escuela_colecciones WHERE nombre = ?";
        if (bloquear) sql += " FOR UPDATE";
        var json = jdbc.queryForObject(sql, String.class, nombre);
        return List.of(mapper.readValue(json, tipo));
    }

    @Override
    public <R> R modificar(Function<List<T>, R> cambio) {
        return transaccion.execute(estado -> {
            var anteriores = leer(true);
            var registros = new ArrayList<>(anteriores);
            var resultado = cambio.apply(registros);
            if (!anteriores.equals(registros)) {
                jdbc.update("UPDATE escuela_colecciones SET contenido = CAST(? AS jsonb) WHERE nombre = ?",
                    mapper.writeValueAsString(registros), nombre);
            }
            return resultado;
        });
    }
}
