package com.claudeproducts.api.product.api;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Envelope de paginação do contrato v1. {@code page} começa em 0. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

	static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
		return new PageResponse<>(page.map(mapper).getContent(), page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}

}
