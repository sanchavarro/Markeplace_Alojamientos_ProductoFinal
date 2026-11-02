package co.edu.unbosque.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.service.api.AlojamientoServiceAPI;

/**
 * Sirve las fotos subidas de los alojamientos (HU-07, HU-15). Público.
 */
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
@RestController
@RequestMapping("/imagenes")
public class ImagenRestController {

    @Autowired
    private AlojamientoServiceAPI alojamientoServiceAPI;

    // GET /imagenes/{nombre}
    @GetMapping("/{nombre}")
    public ResponseEntity<?> ver(@PathVariable String nombre) {
        byte[] datos = alojamientoServiceAPI.leerImagen(nombre);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(alojamientoServiceAPI.tipoImagen(nombre)))
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7)))
                .body(datos);
    }
}
