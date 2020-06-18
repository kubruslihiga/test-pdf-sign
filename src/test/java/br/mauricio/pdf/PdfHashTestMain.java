package br.mauricio.pdf;

import java.util.Base64;

public class PdfHashTestMain {

	public static void main(String[] args) {
		byte[] decodedSignature = Base64.getDecoder().decode("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");
		System.out.println(decodedSignature);
	}
}