package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.api.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EffectVerificationStoreAccessServiceTests {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private Store store;

    @Mock
    private User owner;

    @Test
    void allowsStoreOwner() {
        EffectVerificationStoreAccessService service = service(false);
        when(storeRepository.findById(3L)).thenReturn(Optional.of(store));
        when(store.getOwner()).thenReturn(owner);
        when(owner.getId()).thenReturn(10L);

        assertThatCode(() -> service.validateOwner(10L, 3L))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsOtherStoreOwner() {
        EffectVerificationStoreAccessService service = service(false);
        when(storeRepository.findById(3L)).thenReturn(Optional.of(store));
        when(store.getOwner()).thenReturn(owner);
        when(owner.getId()).thenReturn(11L);

        assertThatThrownBy(() -> service.validateOwner(10L, 3L))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.FORBIDDEN)
                );
    }

    @Test
    void skipsOwnershipCheckForMockProfile() {
        EffectVerificationStoreAccessService service = service(true);

        assertThatCode(() -> service.validateOwner(null, 3L))
                .doesNotThrowAnyException();
        verify(storeRepository, never()).findById(3L);
    }

    @Test
    void resolvesAuthenticatedOwnersOnlyStore() {
        EffectVerificationStoreAccessService service = service(true);
        when(storeRepository.findByOwnerId(10L)).thenReturn(Optional.of(store));
        when(store.getId()).thenReturn(1L);

        assertThat(service.resolveOwnedStoreId(10L, 2L)).isEqualTo(1L);
    }

    @Test
    void keepsRequestedStoreOnlyForUnauthenticatedMockCall() {
        EffectVerificationStoreAccessService service = service(true);

        assertThat(service.resolveOwnedStoreId(null, 2L)).isEqualTo(2L);
        verify(storeRepository, never()).findByOwnerId(10L);
    }

    private EffectVerificationStoreAccessService service(boolean mockPublicAccess) {
        EffectVerificationStoreAccessService service =
                new EffectVerificationStoreAccessService(storeRepository);
        ReflectionTestUtils.setField(
                service,
                "mockPublicAccess",
                mockPublicAccess
        );
        return service;
    }
}
