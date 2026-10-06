package com.claudeproducts.api.product.domain;

public class DuplicateProductNameException extends RuntimeException {

	public DuplicateProductNameException(String name) {
		super("Já existe um produto com o nome \"" + name + "\".");
	}

}
