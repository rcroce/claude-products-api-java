package com.claudeproducts.api.product;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.claudeproducts.api.TestcontainersConfiguration;
import com.claudeproducts.api.product.domain.Product;
import com.claudeproducts.api.product.persistence.ProductRepository;

/** Garantias que ficam no banco, mesmo que o código da API falhe em checar. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ProductRepositoryTest {

	@Autowired
	ProductRepository repository;

	@Test
	void uniqueNameIgnoresCase() {
		repository.saveAndFlush(new Product("Mouse", 1));

		assertThatThrownBy(() -> repository.saveAndFlush(new Product("MOUSE", 2)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void quantityOutOfRangeIsRejected() {
		assertThatThrownBy(() -> repository.saveAndFlush(new Product("Teclado", -1)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void blankNameIsRejected() {
		assertThatThrownBy(() -> repository.saveAndFlush(new Product("   ", 1)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

}
