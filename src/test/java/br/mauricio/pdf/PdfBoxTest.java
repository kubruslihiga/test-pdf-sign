package br.mauricio.pdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PdfBoxTest {

	@Test
	public void testPdfBox() throws Exception {
		PDDocument document = PDDocument.load(new File("src/test/resources/pdf_input.pdf"));
		PDSignature signature = new PDSignature();
		signature.setName("Nao sei");
		signature.setLocation("Bla");
		signature.setContactInfo("Contato");
		signature.setReason("Nao interessa");
		signature.setSignDate(Calendar.getInstance());
		signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
		signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
		document.addSignature(signature);
		FileOutputStream output = new FileOutputStream("src/test/resources/pdf_output.pdf");
		ExternalSigningSupport support = document.saveIncrementalForExternalSigning(output);
		try (InputStream content = support.getContent()) {
			System.out.println(toHexString(hashSHA256(IOUtils.toByteArray(content)))); // enviar o conteudo para webservice do birdID para assinar
			support.setSignature(null); // substituir o null pelo byte array da assinatura do conteudo
		} finally {
			output.close();
		}
	}
	
	private byte[] hashSHA256(byte[] in) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.update(in);
			return digest.digest();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

	}

	private String toHexString(byte[] hash) {
		// Convert byte array into signum representation
		BigInteger number = new BigInteger(1, hash);
		// Convert message digest into hex value
		StringBuilder hexString = new StringBuilder(number.toString(16));
		// Pad with leading zeros
		while (hexString.length() < 32) {
			hexString.insert(0, '0');
		}

		return hexString.toString();
	}
}
