package cl.marcelapaz;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SaludController {
    @GetMapping("/api/health")
    Map<String, String> salud() {
        return Map.of("estado", "ok");
    }
}
