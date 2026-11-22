package cl.duoc.atenasapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.atenasapi.domain.RolUsuario;
import cl.duoc.atenasapi.dto.LoginRequest;
import cl.duoc.atenasapi.dto.UsuarioResponse;
import cl.duoc.atenasapi.exception.CredencialesInvalidasException;
import cl.duoc.atenasapi.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pruebas del endpoint de inicio de sesión.
 *
 * @WebMvcTest levanta solo la capa web (controlador y manejador de errores),
 * sin servidor ni base de datos. El servicio se simula: aquí no se prueba si
 * la clave es correcta, sino que la API responda el código y el JSON que la
 * app espera en cada caso.
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService servicio;

    @Test
    void credencialesCorrectasDevuelvenElUsuarioConSuRolYSinLaClave() throws Exception {
        when(servicio.iniciarSesion(any(LoginRequest.class))).thenReturn(
            new UsuarioResponse(1L, "cvaldes", "Camila Valdés", RolUsuario.EJECUTIVO, "Convenios corporativos",
                "Región Metropolitana", true, 0L, 0L)
        );

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombreUsuario": "cvaldes", "clave": "atenas123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombreCompleto").value("Camila Valdés"))
            .andExpect(jsonPath("$.rol").value("EJECUTIVO"))
            .andExpect(jsonPath("$.claveHash").doesNotExist());
    }

    @Test
    void credencialesIncorrectasResponden401SinDecirQueCampoFallo() throws Exception {
        when(servicio.iniciarSesion(any(LoginRequest.class))).thenThrow(new CredencialesInvalidasException());

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombreUsuario": "cvaldes", "clave": "otra"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.estado").value(401))
            .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos"));
    }

    @Test
    void camposVaciosResponden400ConElErrorDeCadaCampoYNoConsultanAlServicio() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombreUsuario": "", "clave": ""}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.nombreUsuario").value("El usuario es obligatorio"))
            .andExpect(jsonPath("$.campos.clave").value("La contraseña es obligatoria"));

        verify(servicio, never()).iniciarSesion(any());
    }
}
