package br.mauricio.pdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
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
			byte[] contentSignature = sign(content); // vc nao precisa assinar com isso
			System.out.println(toHexString(hashSHA256(IOUtils.toByteArray(content)))); // enviar o conteudo para
																						// webservice do birdID para
																						// assinar
			support.setSignature(contentSignature); // substituir o contentSignature pelo byte array da assinatura do conteudo
		} finally {
			output.close();
		}
	}

	public byte[] sign(InputStream content) throws Exception {
		// cannot be done private (interface)
		KeyStore keystore = KeyStore.getInstance("PKCS12");
		char[] password = "123456".toCharArray();
		keystore.load(new FileInputStream("keystore.p12"), password);
		X509Certificate cert = (X509Certificate) keystore.getCertificate("test");
		PrivateKey privateKey = ((PrivateKey) keystore.getKey("test", password));
		Certificate[] certificateChain = keystore.getCertificateChain("test");
		CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
		ContentSigner sha1Signer = new JcaContentSignerBuilder("SHA256WithRSA").build(privateKey);
		gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build())
				.build(sha1Signer, cert));
		gen.addCertificates(new JcaCertStore(Arrays.asList(certificateChain)));
		CMSProcessableInputStream msg = new CMSProcessableInputStream(content);
		CMSSignedData signedData = gen.generate(msg, false);
//			if (tsaUrl != null && tsaUrl.length() > 0) {
//				ValidationTimeStamp validation = new ValidationTimeStamp(tsaUrl);
//				signedData = validation.addSignedTimeStamp(signedData);
//			}
		return signedData.getEncoded();
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

/**
 * Wraps a InputStream into a CMSProcessable object for bouncy castle. It's a
 * memory saving alternative to the
 * {@link org.bouncycastle.cms.CMSProcessableByteArray CMSProcessableByteArray}
 * class.
 *
 * @author Thomas Chojecki
 */
class CMSProcessableInputStream implements CMSTypedData {
	private InputStream in;
	private final ASN1ObjectIdentifier contentType;

	CMSProcessableInputStream(InputStream is) {
		this(new ASN1ObjectIdentifier(CMSObjectIdentifiers.data.getId()), is);
	}

	CMSProcessableInputStream(ASN1ObjectIdentifier type, InputStream is) {
		contentType = type;
		in = is;
	}

	@Override
	public Object getContent() {
		return in;
	}

	@Override
	public void write(OutputStream out) throws IOException, CMSException {
		// read the content only one time
		IOUtils.copy(in, out);
		in.close();
	}

	@Override
	public ASN1ObjectIdentifier getContentType() {
		return contentType;
	}
}