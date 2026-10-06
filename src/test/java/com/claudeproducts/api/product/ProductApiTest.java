package com.claudeproducts.api.product;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.claudeproducts.api.TestcontainersConfiguration;
import com.claudeproducts.api.product.domain.Product;
import com.claudeproducts.api.product.persistence.ProductRepository;

/**
 * Testes de ponta a ponta da API contra um Postgres real. Toda resposta é
 * conferida contra o contrato em {@code static/openapi.yaml}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProductApiTest {

	private static final String PRODUCTS = "/api/v1/products";

	/** Só as respostas são validadas: vários testes enviam requisições inválidas de propósito. */
	private static final OpenApiInteractionValidator CONTRACT = OpenApiInteractionValidator
		.createForSpecificationUrl("static/openapi.yaml")
		.withBasePathOverride("/api/v1")
		.withLevelResolver(LevelResolver.create().withLevel("validation.request", ValidationReport.Level.IGNORE).build())
		.build();

	@Autowired
	MockMvc mvc;

	@Autowired
	ProductRepository repository;

	Product mouse;

	Product keyboard;

	Product monitor;

	@BeforeEach
	void seed() {
		repository.deleteAll();
		mouse = repository.save(new Product("Mouse", 50));
		keyboard = repository.save(new Product("Teclado", 5));
		monitor = repository.save(new Product("Monitor", 35));
	}

	private static ResultMatcher contract() {
		return OpenApiValidationMatchers.openApi().isValid(CONTRACT);
	}

	private ResultActions send(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
		return mvc.perform(request).andExpect(contract());
	}

	private static String json(String name, Object quantity) {
		return "{\"name\":" + (name == null ? "null" : "\"" + name + "\"") + ",\"quantity\":" + quantity + "}";
	}

	@Nested
	class List {

		@Test
		void returnsFirstPageSortedByName() throws Exception {
			send(get(PRODUCTS)).andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].name", contains("Monitor", "Mouse", "Teclado")))
				.andExpect(jsonPath("$.page", is(0)))
				.andExpect(jsonPath("$.size", is(20)))
				.andExpect(jsonPath("$.totalItems", is(3)))
				.andExpect(jsonPath("$.totalPages", is(1)));
		}

		@Test
		void searchesByNameIgnoringCase() throws Exception {
			send(get(PRODUCTS).param("q", "MO")).andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].name", contains("Monitor", "Mouse")));
		}

		@Test
		void treatsLikeWildcardsAsText() throws Exception {
			send(get(PRODUCTS).param("q", "%")).andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(0)));
		}

		@Test
		void paginatesAndSortsDescending() throws Exception {
			send(get(PRODUCTS).param("size", "2").param("page", "1").param("sort", "-quantity"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].name", contains("Teclado")))
				.andExpect(jsonPath("$.totalPages", is(2)));
		}

		@Test
		void rejectsPageSizeAboveLimit() throws Exception {
			send(get(PRODUCTS).param("size", "101")).andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail", is("Tamanho da página deve ser entre 1 e 100.")))
				.andExpect(jsonPath("$.errors[0].field", is("size")));
		}

		@Test
		void rejectsUnknownSort() throws Exception {
			send(get(PRODUCTS).param("sort", "price")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field", is("sort")));
		}

	}

	@Nested
	class Get {

		@Test
		void returnsProduct() throws Exception {
			send(get(PRODUCTS + "/{id}", monitor.getId())).andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("Monitor")))
				.andExpect(jsonPath("$.quantity", is(35)))
				.andExpect(jsonPath("$.version", is(0)));
		}

		@Test
		void returns404WhenMissing() throws Exception {
			send(get(PRODUCTS + "/{id}", 999_999)).andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title", is("Produto não encontrado")))
				.andExpect(jsonPath("$.detail", is("Produto 999999 não encontrado.")));
		}

		@Test
		void returns400ForNonNumericId() throws Exception {
			send(get(PRODUCTS + "/abc")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field", is("id")));
		}

	}

	@Nested
	class Create {

		@Test
		void returns201WithBodyAndLocation() throws Exception {
			send(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(json("  Notebook  ", 10)))
				.andExpect(status().isCreated())
				.andExpect(header().string(HttpHeaders.LOCATION, containsString(PRODUCTS + "/")))
				.andExpect(jsonPath("$.name", is("Notebook")))
				.andExpect(jsonPath("$.quantity", is(10)))
				.andExpect(jsonPath("$.version", is(0)));
		}

		@Test
		void rejectsDuplicateNameIgnoringCase() throws Exception {
			send(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(json("mouse", 1)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title", is("Nome já cadastrado")))
				.andExpect(jsonPath("$.detail", is("Já existe um produto com o nome \"mouse\".")));
		}

		@Test
		void rejectsBlankName() throws Exception {
			send(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(json("   ", 1)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title", is("Dados inválidos")))
				.andExpect(jsonPath("$.detail", is("Nome é obrigatório.")))
				.andExpect(jsonPath("$.errors[0].field", is("name")));
		}

		@Test
		void rejectsNameLongerThan45() throws Exception {
			send(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(json("x".repeat(46), 1)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", is("Nome deve ter no máximo 45 caracteres.")));
		}

		@Test
		void rejectsNegativeQuantity() throws Exception {
			send(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(json("Cabo", -1)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", is("Quantidade deve ser 0 ou maior.")));
		}

		@Test
		void rejectsQuantityAboveMaximum() throws Exception {
			send(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(json("Cabo", 1_000_000_000)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", is("Quantidade deve ser no máximo 999999999.")));
		}

		@Test
		void rejectsFractionalQuantity() throws Exception {
			send(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(json("Cabo", 1.5)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", is("Quantidade deve ser um número inteiro.")))
				.andExpect(jsonPath("$.errors[0].field", is("quantity")));
		}

		@Test
		void reportsEveryInvalidField() throws Exception {
			send(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(json(null, null)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field", contains("name", "quantity")));
		}

		@Test
		void rejectsMalformedJson() throws Exception {
			send(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", is("Corpo da requisição inválido.")));
		}

	}

	@Nested
	class Update {

		@Test
		void updatesAndIncrementsVersion() throws Exception {
			send(put(PRODUCTS + "/{id}", mouse.getId()).contentType(MediaType.APPLICATION_JSON)
				.content(json("Mouse sem fio", 80)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(mouse.getId().intValue())))
				.andExpect(jsonPath("$.name", is("Mouse sem fio")))
				.andExpect(jsonPath("$.quantity", is(80)))
				.andExpect(jsonPath("$.version", is(1)));
		}

		@Test
		void allowsChangingOnlyTheCaseOfItsOwnName() throws Exception {
			send(put(PRODUCTS + "/{id}", mouse.getId()).contentType(MediaType.APPLICATION_JSON)
				.content(json("MOUSE", 50)))
				.andExpect(status().isOk());
		}

		@Test
		void acceptsCurrentVersion() throws Exception {
			send(put(PRODUCTS + "/{id}", mouse.getId()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Mouse\",\"quantity\":60,\"version\":0}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.version", is(1)));
		}

		@Test
		void rejectsStaleVersion() throws Exception {
			send(put(PRODUCTS + "/{id}", mouse.getId()).contentType(MediaType.APPLICATION_JSON)
				.content(json("Mouse", 60)));

			send(put(PRODUCTS + "/{id}", mouse.getId()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Mouse\",\"quantity\":70,\"version\":0}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title", is("Versão desatualizada")));
		}

		@Test
		void rejectsNameOfAnotherProduct() throws Exception {
			send(put(PRODUCTS + "/{id}", mouse.getId()).contentType(MediaType.APPLICATION_JSON)
				.content(json("teclado", 1)))
				.andExpect(status().isConflict());
		}

		@Test
		void returns404WhenMissing() throws Exception {
			send(put(PRODUCTS + "/{id}", 999_999).contentType(MediaType.APPLICATION_JSON).content(json("X", 1)))
				.andExpect(status().isNotFound());
		}

		@Test
		void rejectsInvalidData() throws Exception {
			send(put(PRODUCTS + "/{id}", mouse.getId()).contentType(MediaType.APPLICATION_JSON)
				.content(json("Mouse", -1)))
				.andExpect(status().isBadRequest());
		}

	}

	@Nested
	class Delete {

		@Test
		void returns204AndRemoves() throws Exception {
			send(delete(PRODUCTS + "/{id}", keyboard.getId())).andExpect(status().isNoContent());
			send(get(PRODUCTS + "/{id}", keyboard.getId())).andExpect(status().isNotFound());
		}

		@Test
		void returns404WhenMissing() throws Exception {
			send(delete(PRODUCTS + "/{id}", 999_999)).andExpect(status().isNotFound());
		}

	}

	@Nested
	class Cors {

		@Test
		void allowsConfiguredOrigin() throws Exception {
			mvc.perform(options(PRODUCTS).header(HttpHeaders.ORIGIN, "http://localhost:5173")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
		}

		@Test
		void exposesLocationHeader() throws Exception {
			mvc.perform(post(PRODUCTS).header(HttpHeaders.ORIGIN, "http://localhost:5173")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("Cabo", 1)))
				.andExpect(status().isCreated())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, endsWith("Location")));
		}

		@Test
		void blocksUnknownOrigin() throws Exception {
			mvc.perform(options(PRODUCTS).header(HttpHeaders.ORIGIN, "https://evil.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
				.andExpect(status().isForbidden());
		}

	}

	@Nested
	class Operations {

		@Test
		void exposesHealth() throws Exception {
			mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status", is("UP")));
		}

		@Test
		void servesContract() throws Exception {
			mvc.perform(get("/openapi.yaml")).andExpect(status().isOk())
				.andExpect(content().string(containsString("openapi: 3.0.3")));
		}

	}

}
