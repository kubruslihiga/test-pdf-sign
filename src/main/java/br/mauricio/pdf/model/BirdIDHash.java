package br.mauricio.pdf.model;

public class BirdIDHash {

	private Integer id;
	
	private String alias;
	
	private String hash;
	
	private String hash_algorithm = "2.16.840.1.101.3.4.2.1";
	
	private String signature_format = "CAdES";

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public String getHash_algorithm() {
		return hash_algorithm;
	}

	public void setHash_algorithm(String hash_algorithm) {
		this.hash_algorithm = hash_algorithm;
	}

	public String getSignature_format() {
		return signature_format;
	}

	public void setSignature_format(String signature_format) {
		this.signature_format = signature_format;
	}
	
}
