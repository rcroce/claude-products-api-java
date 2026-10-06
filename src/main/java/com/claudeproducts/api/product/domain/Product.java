package com.claudeproducts.api.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "product")
public class Product {

	public static final int NAME_MAX_LENGTH = 45;
	public static final int QUANTITY_MAX = 999_999_999;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = NAME_MAX_LENGTH)
	private String name;

	@Column(nullable = false)
	private int quantity;

	@Version
	private long version;

	protected Product() {
	}

	public Product(String name, int quantity) {
		this.name = name;
		this.quantity = quantity;
	}

	void update(String name, int quantity) {
		this.name = name;
		this.quantity = quantity;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getQuantity() {
		return quantity;
	}

	public long getVersion() {
		return version;
	}

}
