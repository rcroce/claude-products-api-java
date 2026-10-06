package com.claudeproducts.api.product.domain;

public class ProductNotFoundException extends RuntimeException {

	public ProductNotFoundException(long id) {
		super("Produto " + id + " não encontrado.");
	}

}
