package cl.duoc.atenasapi.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.atenasapi.exception.ConflictoException;
import cl.duoc.atenasapi.exception.RecursoNoEncontradoException;
import cl.duoc.atenasapi.service.OrganizacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * DELETE /api/organizaciones/{id}: la app muestra al usuario el mensaje del
 * 409, así que se verifica que llegue en el campo "mensaje" del JSON.
 */
@WebMvcTest(OrganizacionController.class)
class OrganizacionControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OrganizacionService servicio;

    @Test
    void eliminarUnaOrganizacionSinOportunidadesResponde204() throws Exception {
        mvc.perform(delete("/api/organizaciones/7"))
            .andExpect(status().isNoContent());

        verify(servicio).eliminar(7L);
    }

    @Test
    void eliminarUnaOrganizacionConOportunidadesResponde409ConElMotivo() throws Exception {
        doThrow(new ConflictoException("Constructora Vértice tiene oportunidades registradas y no se puede eliminar"))
            .when(servicio).eliminar(1L);

        mvc.perform(delete("/api/organizaciones/1"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensaje").value("Constructora Vértice tiene oportunidades registradas y no se puede eliminar"));
    }

    @Test
    void eliminarUnaOrganizacionInexistenteResponde404() throws Exception {
        doThrow(new RecursoNoEncontradoException("La organización 99 no existe")).when(servicio).eliminar(99L);

        mvc.perform(delete("/api/organizaciones/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.mensaje").value("La organización 99 no existe"));
    }
}
