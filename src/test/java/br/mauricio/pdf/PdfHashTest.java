package br.mauricio.pdf;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Calendar;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfDate;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignature;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfSignatureAppearance.RenderingMode;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.codec.Base64;
import com.itextpdf.text.pdf.security.ExternalBlankSignatureContainer;
import com.itextpdf.text.pdf.security.ExternalSignatureContainer;
import com.itextpdf.text.pdf.security.MakeSignature;

import br.mauricio.pdf.model.BirdIDHash;
import br.mauricio.pdf.model.BirdIDSign;

@SpringBootTest
public class PdfHashTest {

	private static final String SIGN_FIELD = "assinatura";

	@Autowired
	private ObjectMapper objectMapper;

	private MockMvc mockMvc;

	@Test
	@Order(1)
	public void testGeneratePdfHash() {
		try (FileOutputStream output = new FileOutputStream("src/test/resources/pdf_sign.pdf")) {
			PdfReader reader = new PdfReader(
					IOUtils.toByteArray(new FileInputStream("src/test/resources/pdf_input.pdf")));
			boolean appendSignature = true;
			PdfStamper signature = PdfStamper.createSignature(reader, output, '\0', null, appendSignature);
			PdfSignatureAppearance appearance = signature.getSignatureAppearance();
			Calendar calendar = Calendar.getInstance();
			appearance.setSignDate(calendar);
			appearance.setVisibleSignature(new Rectangle(10, 50, 120, 10), 1, SIGN_FIELD);
			appearance.setReason("Documento assinado");
			appearance.setLocation("Watermelon tecnologia");
			appearance.setRenderingMode(RenderingMode.DESCRIPTION);
			PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE, PdfName.ADBE_PKCS7_DETACHED);
			dic.setReason(appearance.getReason());
			dic.setLocation(appearance.getLocation());
			dic.setContact(appearance.getContact());
			dic.setDate(new PdfDate(appearance.getSignDate()));
			appearance.setCryptoDictionary(dic);
			ExternalSignatureContainer external = new ExternalBlankSignatureContainer(PdfName.ADOBE_PPKLITE, PdfName.ADBE_PKCS7_DETACHED);
			MakeSignature.signExternalContainer(appearance, external, 32770);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			int read;
			InputStream is = appearance.getRangeStream();
			while ((read = is.read()) != -1) {
				bos.write(read);
			}
			bos.flush();
			byte[] bori = bos.toByteArray();
			String encode = toHexString(hashSHA256(bori));
			signature.setFormFlattening(true);
			signature.close();
			reader.close();
		    BirdIDSign sign = new BirdIDSign();
		    BirdIDHash hash = new BirdIDHash();
		    hash.setAlias("teste");
		    hash.setId(1);
		    hash.setHash(encode);
		    sign.setInclude_chain(true);
		    sign.setCertificate_alias("alias_certificado");
		    sign.setHashes(Arrays.asList(hash));
		    String json = objectMapper.writeValueAsString(sign);
		    ResultActions resultActions = this.mockMvc.perform(post("https://api.birdid.com.br/v0/oauth/signature").contentType(MediaType.APPLICATION_JSON).content(json));
		    MvcResult mvcReturn = resultActions.andReturn();
		    System.out.println(mvcReturn.getResponse().getContentAsString());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	@Order(2)
	public void testSaveSignaturePdf() {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] decodedSignature = Base64.decode("encodedSignature");
            ExternalSignatureContainer container = new ExternalSignatureContainer() {
				@Override
				public byte[] sign(InputStream data) throws GeneralSecurityException {
					return decodedSignature;
				}
				@Override
				public void modifySigningDictionary(PdfDictionary signDic) {
					System.out.println(signDic);
				}
			};
            PdfReader reader = new PdfReader(new FileInputStream("src/test/resources/pdf_sign.pdf"));
            MakeSignature.signDeferred(reader, SIGN_FIELD, baos, container);
            reader.close();
        } catch (IOException | DocumentException | GeneralSecurityException  e) {
            throw new RuntimeException(e);
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
