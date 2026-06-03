package com.nexuserp.inventory.domain.port.in;

import com.nexuserp.inventory.application.query.ProductPageQuery;
import com.nexuserp.inventory.domain.model.Product;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface GetProductUseCase {
    Product getById(UUID id, String tenantId);
    Page<Product> getProducts(ProductPageQuery query);
}
