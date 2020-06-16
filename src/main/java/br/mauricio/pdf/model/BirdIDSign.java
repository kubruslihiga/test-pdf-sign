package br.mauricio.pdf.model;

import java.util.List;

public class BirdIDSign {

	private String certificate_alias;
	
	private List<BirdIDHash> hashes;
	
	private boolean include_chain = true;

	public String getCertificate_alias() {
		return certificate_alias;
	}

	public void setCertificate_alias(String certificate_alias) {
		this.certificate_alias = certificate_alias;
	}

	public List<BirdIDHash> getHashes() {
		return hashes;
	}

	public void setHashes(List<BirdIDHash> hashes) {
		this.hashes = hashes;
	}

	public boolean isInclude_chain() {
		return include_chain;
	}

	public void setInclude_chain(boolean include_chain) {
		this.include_chain = include_chain;
	}
	
}
