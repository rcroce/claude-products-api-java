package com.claudeproducts.api.product.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.claudeproducts.api.product.domain.Product;

/**
 * Dados enviados ao criar ou alterar um produto. Mesmas regras do schema zod
 * do frontend. {@code version} só é considerado na alteração e é opcional.
 */
public record ProductRequest(
		@NotBlank(message = "Nome é obrigatório.")
		@Size(max = Product.NAME_MAX_LENGTH, message = "Nome deve ter no máximo 45 caracteres.")
		String name,

		@NotNull(message = "Quantidade é obrigatória.")
		@Min(value = 0, message = "Quantidade deve ser 0 ou maior.")
		@Max(value = Product.QUANTITY_MAX, message = "Quantidade deve ser no máximo 999999999.")
		Integer quantity,

		@Min(value = 0, message = "Versão inválida.")
		Long version) {

	public ProductRequest {
		name = name == null ? null : name.strip();
	}

}
