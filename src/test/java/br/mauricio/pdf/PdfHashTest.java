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
            byte[] decodedSignature = Base64.decode("MIILkgYJKoZIhvcNAQcCoIILgzCCC38CAQExDzANBglghkgBZQMEAgEFADALBgkq\\nhkiG9w0BBwGgggezMIIHrzCCBZegAwIBAgIIEd4gBglOC6cwDQYJKoZIhvcNAQEL\\nBQAwgYkxCzAJBgNVBAYTAkJSMRMwEQYDVQQKEwpJQ1AtQnJhc2lsMTQwMgYDVQQL\\nEytBdXRvcmlkYWRlIENlcnRpZmljYWRvcmEgUmFpeiBCcmFzaWxlaXJhIHYyMRIw\\nEAYDVQQLEwlBQyBTT0xVVEkxGzAZBgNVBAMTEkFDIFNPTFVUSSBNdWx0aXBsYTAe\\nFw0yMDA2MTUwOTU0MTVaFw0yMTA2MTAyMTUzMDBaMIH2MQswCQYDVQQGEwJCUjET\\nMBEGA1UEChMKSUNQLUJyYXNpbDE0MDIGA1UECxMrQXV0b3JpZGFkZSBDZXJ0aWZp\\nY2Fkb3JhIFJhaXogQnJhc2lsZWlyYSB2MjESMBAGA1UECxMJQUMgU09MVVRJMRsw\\nGQYDVQQLExJBQyBTT0xVVEkgTXVsdGlwbGExFzAVBgNVBAsTDjA5NDYxNjQ3MDAw\\nMTk1MRowGAYDVQQLExFDZXJ0aWZpY2FkbyBQRiBBMzE2MDQGA1UEAxMtUEVEUk8g\\nREUgT0xJVkVJUkEgR1VJTUFSQUVTIExFSVRFOjI2OTkyOTgyODAwMIIBIjANBgkq\\nhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsKoMnIwoX6AnWcga3Abb8w2ebsTEgvgQ\\n5H/UUhWxpuM3dTKphED+7pBdBrSoJ0g2AOWcgrpZySff7W/TB0Sc+oG/1Tyd7/Uc\\nH+ZM9u6wmgGsoxrla7shygb03CPMFapDKmnrK4KuONQci2ff59NikcYUYsj3EDSz\\n8IjQRbEIc4ER0YQbEUGs06a5EMN0ep0+5/N7SOk6YrSU7SzBkNp6m9CzY4t4TuwE\\naUWdMSR23tmU66TwmqbCMbVPJ8eiGAmaL4I8YHepHsBXyYp1tIlUgfuEGIAwYD/8\\nRQcf/JJ4IYybDsKuIU0Tt0c5Qrw557yRcMEnVOAo6ixrIMYXv4dm+QIDAQABo4IC\\nqjCCAqYwVAYIKwYBBQUHAQEESDBGMEQGCCsGAQUFBzAChjhodHRwOi8vY2NkLmFj\\nc29sdXRpLmNvbS5ici9sY3IvYWMtc29sdXRpLW11bHRpcGxhLXYxLnA3YjAdBgNV\\nHQ4EFgQUo3Wr4MeDJgon7BdHYsmEuunTu7QwCQYDVR0TBAIwADAfBgNVHSMEGDAW\\ngBQ1rjEU9l7Sek9Y/jSoGmeXCsSbBzBeBgNVHSAEVzBVMFMGBmBMAQIDJTBJMEcG\\nCCsGAQUFBwIBFjtodHRwczovL2NjZC5hY3NvbHV0aS5jb20uYnIvZG9jcy9kcGMt\\nYWMtc29sdXRpLW11bHRpcGxhLnBkZjCB3gYDVR0fBIHWMIHTMD6gPKA6hjhodHRw\\nOi8vY2NkLmFjc29sdXRpLmNvbS5ici9sY3IvYWMtc29sdXRpLW11bHRpcGxhLXYx\\nLmNybDA/oD2gO4Y5aHR0cDovL2NjZDIuYWNzb2x1dGkuY29tLmJyL2xjci9hYy1z\\nb2x1dGktbXVsdGlwbGEtdjEuY3JsMFCgTqBMhkpodHRwOi8vcmVwb3NpdG9yaW8u\\naWNwYnJhc2lsLmdvdi5ici9sY3IvQUNTT0xVVEkvYWMtc29sdXRpLW11bHRpcGxh\\nLXYxLmNybDAOBgNVHQ8BAf8EBAMCBeAwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsG\\nAQUFBwMEMIGSBgNVHREEgYowgYeBEnBvZ2xlaXRlQGdtYWlsLmNvbaA4BgVgTAED\\nAaAvEy0wMTA3MTk3OTI2OTkyOTgyODAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAw\\nMDCgFwYFYEwBAwagDhMMMDAwMDAwMDAwMDAwoB4GBWBMAQMFoBUTEzAwMDAwMDAw\\nMDAwMDAwMDAwMDAwDQYJKoZIhvcNAQELBQADggIBABcyLg8zA4xrM+Z9XiFLHCCM\\nELo4vTEwKkK8ULFIQxGTVnhN4L6j+pQVW8B7Hp3Yoen1RT5AoS8uQ6NjqulfErDc\\nfTzt1l6H/tJ+WQcSx3XA74oDL/ic8xg/Eza+X4XWfNkqd6MW0XVrmXQcVvQz2TgK\\nt01YfCXDh1h/xbaHKUd+oPiEqT83VW7/X3LAI2dlr5YhjEG9jKbuahDzv6Qt8mQa\\nxS3TmOBq4vmxPlOf0YiX1Sz7v2+DleUgJkbFrvaTn3aBtw65O5OO0A+nmAMYCYDB\\nOm/714YgpqWfYMUX2ljiRhfH9w9GLdrBjqLCtqJeo3N8vm4tiDK8jwKq3ZysXq7K\\nyGgS87BDHXkYTYpzjEp18DsgY0K5uA0fTHYIwYveooDxmPucVWCNChbFEjJDnvfh\\nZXZh1MtPN890djfet7rbp+B92xZtVdtouRTmvMhlm1qu+vfhflRsDEje7GljT/n9\\nX3AA8pmC4tZH3siS2cbfXvmZUrhZ7DcMcTNBQPBDC0zfZ6B5buoLk4QC/pgA+LDG\\nMt9m9mF/RmuZ5lwThe+9W2fvgtTD0WkCCinxSn/tpJkslPD12c0zoehfIEEACee3\\n1ncL95FW/9BGUz0lMXCuvB4lYkOAKBVcnLkVQh5htLsEnIE8ilRPPtgDc7I86s1q\\nV1lmOel9sOkZT4hXWkaTMYIDozCCA58CAQEwgZYwgYkxCzAJBgNVBAYTAkJSMRMw\\nEQYDVQQKEwpJQ1AtQnJhc2lsMTQwMgYDVQQLEytBdXRvcmlkYWRlIENlcnRpZmlj\\nYWRvcmEgUmFpeiBCcmFzaWxlaXJhIHYyMRIwEAYDVQQLEwlBQyBTT0xVVEkxGzAZ\\nBgNVBAMTEkFDIFNPTFVUSSBNdWx0aXBsYQIIEd4gBglOC6cwDQYJYIZIAWUDBAIB\\nBQCgggHdMBgGCSqGSIb3DQEJAzELBgkqhkiG9w0BBwEwHAYJKoZIhvcNAQkFMQ8X\\nDTIwMDYxNjIwMTAzNVowLwYJKoZIhvcNAQkEMSIEIBw7h8TieeWULWjCm82qID3Q\\nxAF1+0l5RVyWJr64270iMIGUBgsqhkiG9w0BCRACDzGBhDCBgQYIYEwBBwEBAgIw\\nLzALBglghkgBZQMEAgEEIA9vosYoGYFxbJXHmJkDmERSOxxhwsliKJzax4Ef7uKe\\nMEQwQgYLKoZIhvcNAQkQBQEWM2h0dHA6Ly9wb2xpdGljYXMuaWNwYnJhc2lsLmdv\\ndi5ici9QQV9BRF9SQl92Ml8yLmRlcjCB2gYLKoZIhvcNAQkQAi8xgcowgccwgcQw\\ngcEEIMBGjkKLAGQMZZvbuXCilScbkJV3jo1wb2v1x2dls7/DMIGcMIGPpIGMMIGJ\\nMQswCQYDVQQGEwJCUjETMBEGA1UEChMKSUNQLUJyYXNpbDE0MDIGA1UECxMrQXV0\\nb3JpZGFkZSBDZXJ0aWZpY2Fkb3JhIFJhaXogQnJhc2lsZWlyYSB2MjESMBAGA1UE\\nCxMJQUMgU09MVVRJMRswGQYDVQQDExJBQyBTT0xVVEkgTXVsdGlwbGECCBHeIAYJ\\nTgunMA0GCSqGSIb3DQEBCwUABIIBAG75R/6n6bdM920pj93tLYetxu372cbIKqpI\\njxs9/DuQVkeCvM5JBxcGeVeTpGJ9hMPrxVpqpmZl436yDYtVu03C9/glW3yX87BX\\nHu5BJ1vBzIuRb3SXzDY/HPZpBp61khtpCx04uDxDmbEV3uHI+vDUmFp6iElN9mP4\\nfxoS+1llU2ETGcuReeHixFNNOnAfV44n6kdCva09HB54dtmSbFUb4zuw8DdbdPCZ\\nsBH7JdVnPKJengmoPvqEaOOA9WBBYwrGMms/wyCpJyPxoln6mFRwgggxnnh1LLuc\\nMlauwcNQQZGdD7z7lREZcCms/Eo2BihsZMZ0YtTr0qs8j7+3ka4=");
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
