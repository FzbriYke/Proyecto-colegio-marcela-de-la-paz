package cl.marcelapaz;

import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Profile("salas")
@RestController
// Publica el catálogo compartido de salas desde el perfil salas.
@RequestMapping("/api/salas")
class SalasController {
    record Sala(UUID id, String nombre, int capacidad, String ubicacion) {}
    record NuevaSala(String nombre, int capacidad, String ubicacion) {}
    record CambioRol(UUID credencial, Roles.Rol rol) {}

    private final ColeccionJson<Sala> archivo;
    private final Roles roles;

    SalasController(Almacenes almacenes, Roles roles) {
        this.roles = roles;
        archivo = almacenes.coleccion("salas", Sala[].class);
    }

    // Devuelve todas las salas que se pueden reservar.
    @GetMapping
    synchronized List<Sala> listar() {
        return archivo.leer();
    }

    // Permite que el servicio de reservas valide un identificador de sala.
    @GetMapping("/{id}")
    synchronized Sala buscar(@PathVariable UUID id) {
        return buscar(id, archivo.leer());
    }

    private Sala buscar(UUID id, List<Sala> salas) {
        return salas.stream().filter(sala -> sala.id().equals(id)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sala inexistente"));
    }

    // Informa el rol asignado a la cuenta sin exponer otras credenciales.
    @GetMapping("/rol")
    Roles.Rol rol(@RequestHeader(value = "X-Reserva-Token", required = false) UUID token) {
        return roles.consultar(token);
    }

    @PutMapping("/rol")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void asignarRol(@RequestBody CambioRol cambio,
                    @RequestHeader(value = "X-Reserva-Token", required = false) UUID token) {
        roles.asignar(token, cambio.credencial(), cambio.rol());
    }

    @PostMapping("/rol/docente")
    Roles.Rol registrarDocente(@RequestHeader("X-Reserva-Token") UUID token) {
        return roles.registrarDocente(token);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    synchronized Sala crear(@RequestBody NuevaSala entrada,
                            @RequestHeader(value = "X-Reserva-Token", required = false) UUID token) {
        roles.exigir(token, Roles.Rol.INSPECTOR);
        return archivo.modificar(salas -> {
            var sala = validar(UUID.randomUUID(), entrada, salas);
            salas.add(sala);
            return sala;
        });
    }

    // Actualiza la sala manteniendo el identificador de sus reservas.
    @PutMapping("/{id}")
    synchronized Sala modificar(@PathVariable UUID id, @RequestBody NuevaSala entrada,
                                @RequestHeader(value = "X-Reserva-Token", required = false) UUID token) {
        roles.exigir(token, Roles.Rol.INSPECTOR);
        return archivo.modificar(salas -> {
            var anterior = buscar(id, salas);
            var nueva = validar(id, entrada, salas);
            salas.set(salas.indexOf(anterior), nueva);
            return nueva;
        });
    }

    private Sala validar(UUID id, NuevaSala entrada, List<Sala> salas) {
        String nombre = entrada.nombre() == null ? "" : entrada.nombre().strip();
        String ubicacion = entrada.ubicacion() == null ? "" : entrada.ubicacion().strip();
        if (nombre.isEmpty() || nombre.length() > 80 || ubicacion.length() > 120 || entrada.capacidad() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revisa el nombre, la capacidad y la ubicación");
        }
        if (salas.stream().anyMatch(sala -> !sala.id().equals(id) && sala.nombre().equalsIgnoreCase(nombre))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una sala con ese nombre");
        }
        return new Sala(id, nombre, entrada.capacidad(), ubicacion);
    }
}
