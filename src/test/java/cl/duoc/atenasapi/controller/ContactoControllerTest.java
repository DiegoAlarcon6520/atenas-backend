package cl.duoc.atenasapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.atenasapi.exception.ConflictoException;
import cl.duoc.atenasapi.service.ContactoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Endpoints de contactos: formato de los datos y borrado protegido. */
@WebMvcTest(ContactoController.class)
class ContactoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ContactoService servicio;

    @Test
    void unContactoConCorreoYTelefonoMalEscritosResponde400() throws Exception {
        mvc.perform(post("/api/contactos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"organizacionId": 1, "nombre": "Pedro Rojas", "cargo": "Encargado de eventos",
                     "area": "EVENTOS", "correo": "sin-arroba", "telefono": "123", "medioPreferente": "CORREO"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.correo").value("Ingresa un correo con formato válido"))
            .andExpect(jsonPath("$.campos.telefono").value("Usa el formato +56 9 1234 5678"));

        verify(servicio, never()).crear(any());
    }

    @Test
    void eliminarUnContactoSinOportunidadesResponde204() throws Exception {
        mvc.perform(delete("/api/contactos/8"))
            .andExpect(status().isNoContent());

        verify(servicio).eliminar(8L);
    }

    @Test
    void eliminarUnContactoAsignadoAOportunidadesResponde409() throws Exception {
        doThrow(new ConflictoException("Ignacia Morales está asignado a oportunidades: desactívalo en vez de eliminarlo"))
            .when(servicio).eliminar(1L);

        mvc.perform(delete("/api/contactos/1"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensaje").value("Ignacia Morales está asignado a oportunidades: desactívalo en vez de eliminarlo"));
    }
}
