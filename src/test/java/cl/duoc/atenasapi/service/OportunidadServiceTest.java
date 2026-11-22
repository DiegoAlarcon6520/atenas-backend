package cl.duoc.atenasapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.atenasapi.entity.Oportunidad;
import cl.duoc.atenasapi.exception.ConflictoException;
import cl.duoc.atenasapi.exception.RecursoNoEncontradoException;
import cl.duoc.atenasapi.repository.EvidenciaRepository;
import cl.duoc.atenasapi.repository.OportunidadRepository;
import cl.duoc.atenasapi.repository.SeguimientoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Eliminar una oportunidad: solo se permite mientras no tenga gestiones,
 * porque su historial es la trazabilidad comercial. La app depende de esta
 * regla (muestra el 409 al usuario), así que se prueba en el servidor.
 */
@ExtendWith(MockitoExtension.class)
class OportunidadServiceTest {

    @Mock
    private OportunidadRepository oportunidades;

    @Mock
    private SeguimientoRepository seguimientos;

    @Mock
    private EvidenciaRepository evidencias;

    @Mock
    private Referencias referencias;

    @InjectMocks
    private OportunidadService servicio;

    @Test
    void unaOportunidadConGestionesNoSeElimina() {
        when(oportunidades.findById(100L)).thenReturn(Optional.of(oportunidad()));
        when(seguimientos.existsByOportunidadId(100L)).thenReturn(true);

        ConflictoException error = assertThrows(ConflictoException.class, () -> servicio.eliminar(100L));

        assertEquals("La oportunidad tiene gestiones registradas: descártala en vez de eliminarla", error.getMessage());
        verify(evidencias, never()).deleteByOportunidadId(anyLong());
        verify(oportunidades, never()).delete(any());
    }

    @Test
    void sinGestionesSeEliminanPrimeroSusEvidenciasYLuegoLaOportunidad() {
        Oportunidad oportunidad = oportunidad();
        when(oportunidades.findById(100L)).thenReturn(Optional.of(oportunidad));
        when(seguimientos.existsByOportunidadId(100L)).thenReturn(false);

        servicio.eliminar(100L);

        // Las evidencias apuntan a la oportunidad: si se borrara antes, la llave foránea lo impediría.
        InOrder orden = inOrder(evidencias, oportunidades);
        orden.verify(evidencias).deleteByOportunidadId(100L);
        orden.verify(oportunidades).delete(oportunidad);
    }

    @Test
    void eliminarUnaOportunidadInexistenteResponde404() {
        when(oportunidades.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.eliminar(99L));

        verify(oportunidades, never()).delete(any());
    }

    private static Oportunidad oportunidad() {
        Oportunidad o = new Oportunidad();
        o.setId(100L);
        o.setNombre("Feria de bienestar Vértice");
        return o;
    }
}
