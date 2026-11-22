package cl.duoc.atenasapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.atenasapi.entity.Organizacion;
import cl.duoc.atenasapi.exception.ConflictoException;
import cl.duoc.atenasapi.exception.RecursoNoEncontradoException;
import cl.duoc.atenasapi.repository.ContactoRepository;
import cl.duoc.atenasapi.repository.OportunidadRepository;
import cl.duoc.atenasapi.repository.OrganizacionRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Eliminar una organización: se rechaza si tiene oportunidades (se perdería
 * el historial comercial); si no, se borra junto con sus contactos.
 */
@ExtendWith(MockitoExtension.class)
class OrganizacionServiceTest {

    @Mock
    private OrganizacionRepository organizaciones;

    @Mock
    private ContactoRepository contactos;

    @Mock
    private OportunidadRepository oportunidades;

    @Mock
    private Referencias referencias;

    @InjectMocks
    private OrganizacionService servicio;

    @Test
    void unaOrganizacionConOportunidadesNoSeElimina() {
        when(organizaciones.findById(7L)).thenReturn(Optional.of(organizacion()));
        when(oportunidades.existsByOrganizacionId(7L)).thenReturn(true);

        ConflictoException error = assertThrows(ConflictoException.class, () -> servicio.eliminar(7L));

        assertEquals("Constructora Vértice tiene oportunidades registradas y no se puede eliminar", error.getMessage());
        verify(contactos, never()).deleteByOrganizacionId(anyLong());
        verify(organizaciones, never()).delete(any());
    }

    @Test
    void sinOportunidadesSeEliminanSusContactosYLuegoLaOrganizacion() {
        Organizacion organizacion = organizacion();
        when(organizaciones.findById(7L)).thenReturn(Optional.of(organizacion));
        when(oportunidades.existsByOrganizacionId(7L)).thenReturn(false);

        servicio.eliminar(7L);

        InOrder orden = inOrder(contactos, organizaciones);
        orden.verify(contactos).deleteByOrganizacionId(7L);
        orden.verify(organizaciones).delete(organizacion);
    }

    @Test
    void eliminarUnaOrganizacionInexistenteResponde404() {
        when(organizaciones.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.eliminar(99L));

        verify(organizaciones, never()).delete(any());
    }

    private static Organizacion organizacion() {
        Organizacion o = new Organizacion();
        o.setId(7L);
        o.setNombre("Constructora Vértice");
        return o;
    }
}
