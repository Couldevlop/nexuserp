package com.nexuserp.inventory.domain.port.in;

import com.nexuserp.inventory.application.command.CreateProductCommand;
import com.nexuserp.inventory.domain.model.Product;

public interface CreateProductUseCase {
    Product createProduct(CreateProductCommand command);
}
