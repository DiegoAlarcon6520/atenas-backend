package cl.duoc.atenasapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.atenasapi.entity.Contacto;
import cl.duoc.atenasapi.exception.ConflictoException;
import cl.duoc.atenasapi.exception.RecursoNoEncontradoException;
import cl.duoc.atenasapi.repository.ContactoRepository;
import cl.duoc.atenasapi.repository.OportunidadRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Eliminar un contacto: si está asignado a oportunidades se rechaza (esas
 * oportunidades quedarían sin interlocutor en su historial) y se sugiere
 * desactivarlo.
 */
@ExtendWith(MockitoExtension.class)
class ContactoServiceTest {

    @Mock
    private ContactoRepository contactos;

    @Mock
    private OportunidadRepository oportunidades;

    @Mock
    private Referencias referencias;

    @InjectMocks
    private ContactoService servicio;

    @Test
    void unContactoAsignadoAOportunidadesNoSeElimina() {
        when(contactos.findById(9L)).thenReturn(Optional.of(contacto()));
        when(oportunidades.existsByContactoId(9L)).thenReturn(true);

        ConflictoException error = assertThrows(ConflictoException.class, () -> servicio.eliminar(9L));

        assertEquals("Ignacia Morales está asignado a oportunidades: desactívalo en vez de eliminarlo", error.getMessage());
        verify(contactos, never()).delete(any());
    }

    @Test
    void unContactoSinOportunidadesSeElimina() {
        Contacto contacto = contacto();
        when(contactos.findById(9L)).thenReturn(Optional.of(contacto));
        when(oportunidades.existsByContactoId(9L)).thenReturn(false);

        servicio.eliminar(9L);

        verify(contactos).delete(contacto);
    }

    @Test
    void eliminarUnContactoInexistenteResponde404() {
        when(contactos.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.eliminar(99L));

        verify(contactos, never()).delete(any());
    }

    private static Contacto contacto() {
        Contacto c = new Contacto();
        c.setId(9L);
        c.setNombre("Ignacia Morales");
        return c;
    }
}
