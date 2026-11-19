package co.edu.unal.lab5seguridad.controller;

import java.security.cert.X509Certificate;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class SaludController {

    @GetMapping("/api/v1/salud")
    public Map<String, String> salud(HttpServletRequest request) {
        String clienteCN = extraerClienteCN(request);
        if (clienteCN != null) {
            // Con mTLS activo, imprime el CN del certificado presentado por el cliente.
            System.out.println("Cliente autenticado por mTLS: " + clienteCN);
        }
        return Map.of("estado", "ok");
    }

    // Lee el certificado X.509 del cliente que Tomcat expone tras el handshake mTLS
    // y devuelve el Common Name (CN) del subject, o null si no hay certificado.
    private String extraerClienteCN(HttpServletRequest request) {
        Object attr = request.getAttribute("jakarta.servlet.request.X509Certificate");
        if (!(attr instanceof X509Certificate[] certs) || certs.length == 0) {
            return null;
        }
        String dn = certs[0].getSubjectX500Principal().getName();
        for (String parte : dn.split(",")) {
            parte = parte.trim();
            if (parte.startsWith("CN=")) {
                return parte.substring(3);
            }
        }
        return dn;
    }
}
