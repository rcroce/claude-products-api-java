package com.claudeproducts.api.product.api;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.claudeproducts.api.product.domain.Product;
import com.claudeproducts.api.product.domain.ProductService;

@RestController
@RequestMapping("/api/v1/products")
class ProductController {

	private final ProductService service;

	ProductController(ProductService service) {
		this.service = service;
	}

	@GetMapping
	PageResponse<ProductResponse> list(
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") @Min(value = 0, message = "Página deve ser 0 ou maior.") int page,
			@RequestParam(defaultValue = "20") @Min(value = 1, message = "Tamanho da página deve ser entre 1 e 100.")
			@Max(value = 100, message = "Tamanho da página deve ser entre 1 e 100.") int size,
			@RequestParam(defaultValue = "name") String sort) {
		var pageable = PageRequest.of(page, size, ProductSort.parse(sort));
		return PageResponse.from(service.list(q, pageable), ProductResponse::from);
	}

	@GetMapping("/{id}")
	ProductResponse get(@PathVariable long id) {
		return ProductResponse.from(service.get(id));
	}

	@PostMapping
	ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
		Product product = service.create(request.name(), request.quantity());
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(product.getId())
			.toUri();
		return ResponseEntity.created(location).body(ProductResponse.from(product));
	}

	@PutMapping("/{id}")
	ProductResponse update(@PathVariable long id, @Valid @RequestBody ProductRequest request) {
		return ProductResponse.from(service.update(id, request.name(), request.quantity(), request.version()));
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@PathVariable long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

}
