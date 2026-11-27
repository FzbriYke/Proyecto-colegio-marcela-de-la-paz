package cl.marcelapaz;

import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
class Almacenes {
    private final Path directorio;
    private final ObjectMapper mapper;
    private final DataSource postgres;

    @Autowired
    Almacenes(@Value("${app.data-dir}") String directorio, ObjectMapper mapper, ObjectProvider<DataSource> postgres) {
        this(Path.of(directorio), mapper, postgres.getIfAvailable());
    }

    Almacenes(String directorio, ObjectMapper mapper) {
        this(Path.of(directorio), mapper, null);
    }

    Almacenes(Path directorio, ObjectMapper mapper, DataSource postgres) {
        this.directorio = directorio;
        this.mapper = mapper;
        this.postgres = postgres;
    }

    <T> ColeccionJson<T> coleccion(String nombre, Class<T[]> tipo) {
        if (postgres != null) return new ColeccionPostgres<>(postgres, nombre, tipo, mapper);
        return new ArchivoJson<>(directorio.resolve(nombre + ".json"), tipo, mapper);
    }
}
