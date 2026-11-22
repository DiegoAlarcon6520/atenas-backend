package cl.duoc.atenasapi.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.atenasapi.dto.SincronizacionResponse;
import cl.duoc.atenasapi.service.SincronizacionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Endpoint que llena la copia local de Room. */
@WebMvcTest(SincronizacionController.class)
class SincronizacionControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SincronizacionService servicio;

    @Test
    void sinIndicarElUsuarioResponde400() throws Exception {
        mvc.perform(get("/api/sincronizacion"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("Falta el parámetro 'usuarioId'"));
    }

    @Test
    void unUsuarioNuevoSinCarteraRecibeListasVaciasYNoNulas() throws Exception {
        when(servicio.instantanea(3L)).thenReturn(new SincronizacionResponse(
            1795000000000L, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        ));

        mvc.perform(get("/api/sincronizacion").param("usuarioId", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.oportunidades").isArray())
            .andExpect(jsonPath("$.oportunidades").isEmpty())
            .andExpect(jsonPath("$.seguimientos").isArray());
    }
}
