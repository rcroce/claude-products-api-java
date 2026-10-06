package com.claudeproducts.api.product.api;

import java.util.Map;

import org.springframework.data.domain.Sort;

import com.claudeproducts.api.shared.InvalidRequestException;

/** Traduz o parâmetro {@code sort} (por exemplo {@code name} ou {@code -quantity}). */
final class ProductSort {

	private static final Map<String, String> FIELDS = Map.of("id", "id", "name", "name", "quantity", "quantity");

	private ProductSort() {
	}

	static Sort parse(String value) {
		boolean descending = value.startsWith("-");
		String field = FIELDS.get(descending ? value.substring(1) : value);
		if (field == null) {
			throw new InvalidRequestException("sort", "Ordenação inválida. Use id, name ou quantity, com - para ordem decrescente.");
		}
		Sort sort = Sort.by(descending ? Sort.Direction.DESC : Sort.Direction.ASC, field);
		// Desempate estável para a paginação não repetir nem pular itens.
		return field.equals("id") ? sort : sort.and(Sort.by("id"));
	}

}
