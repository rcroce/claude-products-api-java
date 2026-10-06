package com.claudeproducts.api.product.api;

import com.claudeproducts.api.product.domain.Product;

public record ProductResponse(long id, String name, int quantity, long version) {

	static ProductResponse from(Product product) {
		return new ProductResponse(product.getId(), product.getName(), product.getQuantity(), product.getVersion());
	}

}
