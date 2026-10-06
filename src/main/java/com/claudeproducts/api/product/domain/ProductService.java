package com.claudeproducts.api.product.domain;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.claudeproducts.api.product.persistence.ProductRepository;

@Service
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository repository;

	public ProductService(ProductRepository repository) {
		this.repository = repository;
	}

	public Page<Product> list(String query, Pageable pageable) {
		if (query == null || query.isBlank()) {
			return repository.findAll(pageable);
		}
		return repository.findByNameContainingIgnoreCase(query.strip(), pageable);
	}

	public Product get(long id) {
		return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
	}

	@Transactional
	public Product create(String name, int quantity) {
		if (repository.existsByNameIgnoreCase(name)) {
			throw new DuplicateProductNameException(name);
		}
		return saveCheckingName(new Product(name, quantity), name);
	}

	/**
	 * Altera o produto. Quando {@code expectedVersion} vem preenchido, recusa a
	 * alteração se outra pessoa mudou o produto depois que o cliente o leu.
	 */
	@Transactional
	public Product update(long id, String name, int quantity, Long expectedVersion) {
		Product product = get(id);
		if (expectedVersion != null && expectedVersion != product.getVersion()) {
			throw new StaleProductException();
		}
		if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw new DuplicateProductNameException(name);
		}
		product.update(name, quantity);
		return saveCheckingName(product, name);
	}

	@Transactional
	public void delete(long id) {
		repository.delete(get(id));
	}

	/** O índice único do banco é a garantia final contra dois cadastros simultâneos. */
	private Product saveCheckingName(Product product, String name) {
		try {
			return repository.saveAndFlush(product);
		}
		catch (DataIntegrityViolationException ex) {
			throw new DuplicateProductNameException(name);
		}
	}

}
