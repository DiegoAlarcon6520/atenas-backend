package cl.duoc.atenasapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.atenasapi.domain.AfinidadPublico;
import cl.duoc.atenasapi.domain.EstadoOportunidad;
import cl.duoc.atenasapi.domain.PotencialComercial;
import cl.duoc.atenasapi.domain.Prioridad;
import cl.duoc.atenasapi.domain.TipoAccion;
import cl.duoc.atenasapi.domain.TipoOportunidad;
import cl.duoc.atenasapi.domain.TransicionesEstado;
import cl.duoc.atenasapi.dto.AsignacionRequest;
import cl.duoc.atenasapi.dto.OportunidadRequest;
import cl.duoc.atenasapi.dto.OportunidadResponse;
import cl.duoc.atenasapi.dto.SeguimientoRequest;
import cl.duoc.atenasapi.dto.SeguimientoResponse;
import cl.duoc.atenasapi.exception.AccionNoPermitidaException;
import cl.duoc.atenasapi.exception.ConflictoException;
import cl.duoc.atenasapi.exception.RecursoNoEncontradoException;
import cl.duoc.atenasapi.service.OportunidadService;
import cl.duoc.atenasapi.service.SeguimientoService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Endpoints de oportunidades y gestiones: que cada regla de negocio llegue a
 * la app con el código HTTP correcto, porque la app decide qué mostrar según
 * ese código (400 = corregir el formulario, 403 = no es tu decisión,
 * 409 = el dato cambió o está cerrado).
 */
@WebMvcTest(OportunidadController.class)
class OportunidadControllerTest {

    private static final String OPORTUNIDAD_VALIDA = """
        {
          "nombre": "Feria de Beneficios Primavera",
          "tipo": "ACTIVIDAD_MUNICIPAL",
          "descripcion": "Feria abierta a vecinos con stands de convenios.",
          "fuenteInformacion": "Sitio web municipal",
          "fechaDeteccion": 1795000000000,
          "fechaInicioEvento": 1796000000000,
          "horaInicio": 600,
          "horaTermino": 1080,
          "direccion": "Plaza Cívica s/n",
          "comunaId": 2,
          "organizacionId": 2,
          "creadoPorId": 1,
          "publicoObjetivo": "Vecinos y familias",
          "publicoEsperado": 2500,
          "prioridad": "ALTA"
        }
        """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OportunidadService oportunidades;

    @MockitoBean
    private SeguimientoService seguimientos;

    // ---------- Consultar ----------

    @Test
    void unaOportunidadQueNoExisteResponde404() throws Exception {
        when(oportunidades.obtener(99L)).thenThrow(new RecursoNoEncontradoException("La oportunidad 99 no existe"));

        mvc.perform(get("/api/oportunidades/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.mensaje").value("La oportunidad 99 no existe"));
    }

    @Test
    void unIdQueNoEsNumeroResponde400() throws Exception {
        mvc.perform(get("/api/oportunidades/abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("El parámetro 'id' no tiene un valor válido"));
    }

    @Test
    void elFiltroPorResponsableLlegaAlServicio() throws Exception {
        when(oportunidades.listar(1L)).thenReturn(List.of(respuesta(EstadoOportunidad.IDENTIFICADA)));

        mvc.perform(get("/api/oportunidades").param("responsableId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].responsableId").value(1));
    }

    // ---------- Crear y editar ----------

    @Test
    void crearUnaOportunidadValidaResponde201ConSuEstadoInicial() throws Exception {
        when(oportunidades.crear(any(OportunidadRequest.class))).thenReturn(respuesta(EstadoOportunidad.IDENTIFICADA));

        mvc.perform(post("/api/oportunidades").contentType(MediaType.APPLICATION_JSON).content(OPORTUNIDAD_VALIDA))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1000))
            .andExpect(jsonPath("$.estado").value("IDENTIFICADA"));
    }

    @Test
    void sinLosCamposObligatoriosResponde400ConCadaErrorYNoGuardaNada() throws Exception {
        mvc.perform(post("/api/oportunidades").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("Revisa los campos marcados"))
            .andExpect(jsonPath("$.campos.nombre").value("El nombre del evento es obligatorio"))
            .andExpect(jsonPath("$.campos.comunaId").value("Selecciona una comuna"))
            .andExpect(jsonPath("$.campos.organizacionId").value("Selecciona una organización"));

        verify(oportunidades, never()).crear(any());
    }

    @Test
    void unPublicoEsperadoDeCeroNoEsUnaOportunidad() throws Exception {
        String conCeroAsistentes = OPORTUNIDAD_VALIDA.replace("\"publicoEsperado\": 2500", "\"publicoEsperado\": 0");

        mvc.perform(post("/api/oportunidades").contentType(MediaType.APPLICATION_JSON).content(conCeroAsistentes))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.publicoEsperado").value("El público esperado debe ser mayor que cero"));
    }

    @Test
    void unValorQueNoExisteEnElCatalogoResponde400() throws Exception {
        String conTipoInventado = OPORTUNIDAD_VALIDA.replace("ACTIVIDAD_MUNICIPAL", "CONCIERTO");

        mvc.perform(post("/api/oportunidades").contentType(MediaType.APPLICATION_JSON).content(conTipoInventado))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("El cuerpo de la solicitud no es válido o tiene un valor no permitido"));
    }

    @Test
    void editarUnaOportunidadCerradaResponde409() throws Exception {
        when(oportunidades.actualizar(eq(7L), any(OportunidadRequest.class)))
            .thenThrow(new ConflictoException(TransicionesEstado.MENSAJE_CERRADA));

        mvc.perform(put("/api/oportunidades/7").contentType(MediaType.APPLICATION_JSON).content(OPORTUNIDAD_VALIDA))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensaje").value(TransicionesEstado.MENSAJE_CERRADA));
    }

    // ---------- Asignar ----------

    @Test
    void unEjecutivoQueIntentaAsignarRecibe403() throws Exception {
        when(oportunidades.asignarResponsable(eq(7L), any(AsignacionRequest.class)))
            .thenThrow(new AccionNoPermitidaException("Solo el supervisor comercial puede asignar responsables"));

        mvc.perform(patch("/api/oportunidades/7/responsable")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"responsableId": 2, "usuarioId": 1}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.mensaje").value("Solo el supervisor comercial puede asignar responsables"));
    }

    // ---------- Gestiones ----------

    @Test
    void registrarUnaGestionValidaResponde201ConElCambioDeEstado() throws Exception {
        when(seguimientos.registrar(eq(7L), any(SeguimientoRequest.class))).thenReturn(
            new SeguimientoResponse(1L, 7L, 1L, TipoAccion.LLAMADA, 1795000000000L, "Se conversó con la jefa de bienestar.",
                null, null, EstadoOportunidad.CONTACTO_PENDIENTE, EstadoOportunidad.CONTACTO_REALIZADO, null)
        );

        mvc.perform(post("/api/oportunidades/7/seguimientos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"usuarioId": 1, "tipoAccion": "LLAMADA", "fechaAccion": 1795000000000,
                     "resultado": "Se conversó con la jefa de bienestar.", "estadoNuevo": "CONTACTO_REALIZADO"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estadoAnterior").value("CONTACTO_PENDIENTE"))
            .andExpect(jsonPath("$.estadoNuevo").value("CONTACTO_REALIZADO"));
    }

    @Test
    void unEjecutivoQueIntentaConfirmarRecibe403() throws Exception {
        when(seguimientos.registrar(eq(7L), any(SeguimientoRequest.class))).thenThrow(new AccionNoPermitidaException(
            "Confirmar, descartar, cerrar o retroceder una oportunidad es una decisión del supervisor comercial"));

        mvc.perform(post("/api/oportunidades/7/seguimientos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"usuarioId": 1, "tipoAccion": "CONFIRMACION", "fechaAccion": 1795000000000,
                     "resultado": "La empresa confirmó por correo.", "estadoNuevo": "CONFIRMADA"}
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void unResultadoDemasiadoCortoResponde400AntesDeLlegarAlServicio() throws Exception {
        mvc.perform(post("/api/oportunidades/7/seguimientos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"usuarioId": 1, "tipoAccion": "LLAMADA", "fechaAccion": 1795000000000, "resultado": "Ok"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.resultado").value("El resultado debe tener entre 10 y 1000 caracteres"));

        verify(seguimientos, never()).registrar(any(), any());
    }

    @Test
    void elHistorialSeEntregaDeLaGestionMasRecienteALaMasAntigua() throws Exception {
        when(seguimientos.historial(7L)).thenReturn(List.of(
            new SeguimientoResponse(2L, 7L, 1L, TipoAccion.ENVIO_PROPUESTA, 1795100000000L, "Propuesta enviada por correo.",
                null, null, EstadoOportunidad.CONTACTO_REALIZADO, EstadoOportunidad.PROPUESTA_ENVIADA, null),
            new SeguimientoResponse(1L, 7L, 1L, TipoAccion.LLAMADA, 1795000000000L, "Primer contacto telefónico.",
                null, null, EstadoOportunidad.CONTACTO_PENDIENTE, EstadoOportunidad.CONTACTO_REALIZADO, null)
        ));

        mvc.perform(get("/api/oportunidades/7/seguimientos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].estadoNuevo").value("PROPUESTA_ENVIADA"))
            .andExpect(jsonPath("$[1].estadoNuevo").value("CONTACTO_REALIZADO"));
    }

    private static OportunidadResponse respuesta(EstadoOportunidad estado) {
        return new OportunidadResponse(
            1000L, "Feria de Beneficios Primavera", TipoOportunidad.ACTIVIDAD_MUNICIPAL,
            "Feria abierta a vecinos con stands de convenios.", "Sitio web municipal",
            1795000000000L, 1796000000000L, null, 600, 1080, "Plaza Cívica s/n",
            2L, 2L, null, 1L, 1L, "Vecinos y familias", 2500, Prioridad.ALTA,
            PotencialComercial.MEDIO, AfinidadPublico.MEDIA, estado, 0.0,
            null, null, null, null, 1795000000000L, 1795000000000L
        );
    }
}
