/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.ProductBranch;
import ru.beeline.fdmproducts.repository.ContainerRepository;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.DiscoveredParameterRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;
import ru.beeline.fdmproducts.repository.LocalAssessmentRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ParameterRepository;
import ru.beeline.fdmproducts.repository.ProductBranchRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;
import ru.beeline.fdmproducts.repository.SlaRepository;
import ru.beeline.fdmproducts.repository.TechProductRepository;
import ru.beeline.fdmproducts.repository.UserProductRepository;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceDeleteProductTest {

    private static final Integer PRODUCT_ID = 1;
    private static final String ALIAS = "orders-product";
    private static final List<Integer> CONTAINER_IDS = List.of(20);
    private static final List<Integer> INTERFACE_IDS = List.of(30);
    private static final List<Integer> OPERATION_IDS = List.of(40);

    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserProductRepository userProductRepository;
    @Mock
    private ProductBranchRepository productBranchRepository;
    @Mock
    private ContainerRepository containerRepository;
    @Mock
    private InterfaceRepository interfaceRepository;
    @Mock
    private OperationRepository operationRepository;
    @Mock
    private SlaRepository slaRepository;
    @Mock
    private ParameterRepository parameterRepository;
    @Mock
    private DiscoveredInterfaceRepository discoveredInterfaceRepository;
    @Mock
    private DiscoveredOperationRepository discoveredOperationRepository;
    @Mock
    private DiscoveredParameterRepository discoveredParameterRepository;
    @Mock
    private TechProductRepository techProductRepository;
    @Mock
    private LocalAssessmentRepository localAssessmentRepository;

    @InjectMocks
    private ProductService service;

    @BeforeEach
    void setUp() {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setAlias(ALIAS);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productBranchRepository.findAllByAlias(ALIAS))
                .thenReturn(List.of(ProductBranch.builder().id(10).alias(ALIAS).branchName("develop").build()));
        when(containerRepository.findIdsByProductBranchIdIn(List.of(10))).thenReturn(CONTAINER_IDS);
        when(interfaceRepository.findIdsByContainerIds(CONTAINER_IDS)).thenReturn(INTERFACE_IDS);
        when(operationRepository.findIdsByInterfaceIds(INTERFACE_IDS)).thenReturn(OPERATION_IDS);
        when(discoveredInterfaceRepository.findIdsByInterfaceIds(INTERFACE_IDS)).thenReturn(List.of());
        when(discoveredOperationRepository.findIdsByOperationIds(OPERATION_IDS)).thenReturn(List.of());
        when(localAssessmentRepository.findIdsByProductId(PRODUCT_ID)).thenReturn(List.of());
    }

    @Test
    @DisplayName("T0781#test006: сбор по operation-атрибутам всегда, даже без discovered-интерфейсов")
    void deletesDiscoveredOperationsFoundByOperationWithoutDiscoveredInterface() {
        when(discoveredOperationRepository.findIdsByOperationIds(OPERATION_IDS)).thenReturn(List.of(50));

        service.deleteProduct(PRODUCT_ID);

        InOrder inOrder = inOrder(discoveredParameterRepository, discoveredOperationRepository, operationRepository,
                interfaceRepository, discoveredInterfaceRepository, productRepository);
        inOrder.verify(discoveredParameterRepository).deleteByDiscoveredOperationIdIn(List.of(50));
        inOrder.verify(discoveredOperationRepository).deleteByIdIn(List.of(50));
        inOrder.verify(operationRepository).deleteByIdIn(OPERATION_IDS);
        inOrder.verify(interfaceRepository).deleteByIdIn(INTERFACE_IDS);
        inOrder.verify(discoveredInterfaceRepository).clearProductId(PRODUCT_ID);
        inOrder.verify(productRepository).deleteById(PRODUCT_ID);
        verify(discoveredOperationRepository).findIdsByOperationIds(OPERATION_IDS);
        verify(discoveredInterfaceRepository, never()).deleteByIdIn(anyList());
    }

    @Test
    @DisplayName("Discovered-интерфейс и его методы удаляются до интерфейса, методы — один раз")
    void deletesDiscoveredInterfaceWithItsOperationsBeforeInterface() {
        when(discoveredInterfaceRepository.findIdsByInterfaceIds(INTERFACE_IDS)).thenReturn(List.of(60));
        when(discoveredOperationRepository.findIdsByDiscoveredInterfaceIds(List.of(60))).thenReturn(List.of(61, 62));
        when(discoveredOperationRepository.findIdsByOperationIds(OPERATION_IDS)).thenReturn(List.of(62, 63));

        service.deleteProduct(PRODUCT_ID);

        InOrder inOrder = inOrder(discoveredParameterRepository, discoveredOperationRepository,
                discoveredInterfaceRepository, slaRepository, operationRepository, interfaceRepository,
                containerRepository, productBranchRepository, productRepository);
        inOrder.verify(discoveredParameterRepository).deleteByDiscoveredOperationIdIn(List.of(61, 62, 63));
        inOrder.verify(discoveredOperationRepository).deleteByIdIn(List.of(61, 62, 63));
        inOrder.verify(discoveredInterfaceRepository).deleteByIdIn(List.of(60));
        inOrder.verify(slaRepository).deleteByOperationIdIn(OPERATION_IDS);
        inOrder.verify(operationRepository).deleteByIdIn(OPERATION_IDS);
        inOrder.verify(interfaceRepository).deleteByIdIn(INTERFACE_IDS);
        inOrder.verify(containerRepository).deleteByIdIn(CONTAINER_IDS);
        inOrder.verify(productBranchRepository).deleteByAlias(ALIAS);
        inOrder.verify(discoveredInterfaceRepository).clearProductId(PRODUCT_ID);
        inOrder.verify(productRepository).deleteById(PRODUCT_ID);
    }

    @Test
    @DisplayName("Продукт без discovered-связей удаляется как раньше")
    void deletesProductWithoutDiscoveredRelations() {
        service.deleteProduct(PRODUCT_ID);

        verify(discoveredParameterRepository, never()).deleteByDiscoveredOperationIdIn(any());
        verify(discoveredOperationRepository, never()).deleteByIdIn(any());
        verify(discoveredInterfaceRepository, never()).deleteByIdIn(any());
        verify(operationRepository).deleteByIdIn(OPERATION_IDS);
        verify(interfaceRepository).deleteByIdIn(INTERFACE_IDS);
        InOrder inOrder = inOrder(discoveredInterfaceRepository, productRepository);
        inOrder.verify(discoveredInterfaceRepository).clearProductId(PRODUCT_ID);
        inOrder.verify(productRepository).deleteById(PRODUCT_ID);
    }

    @Test
    @DisplayName("Без операций продукта поиск discovered по атрибутам операции не выполняется: IN () — ошибка")
    void doesNotSearchDiscoveredOperationsWithoutOperations() {
        when(operationRepository.findIdsByInterfaceIds(INTERFACE_IDS)).thenReturn(List.of());

        service.deleteProduct(PRODUCT_ID);

        verify(discoveredOperationRepository, never()).findIdsByOperationIds(any());
        verify(interfaceRepository).deleteByIdIn(INTERFACE_IDS);
    }
}
