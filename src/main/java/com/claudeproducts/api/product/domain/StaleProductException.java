package com.claudeproducts.api.product.domain;

/** O produto mudou desde que o cliente o leu (versão diferente). */
public class StaleProductException extends RuntimeException {

	public StaleProductException() {
		super("O produto foi alterado por outra pessoa. Recarregue e tente de novo.");
	}

}
