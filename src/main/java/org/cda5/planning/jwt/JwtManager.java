package org.cda5.planning.jwt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;

@ApplicationScoped
public class JwtManager {
	  static {
	        FileInputStream fis = null;
	        char[] password = "secretaire".toCharArray();
	        String alias = "alias";
	        PrivateKey pk = null;
	        try {
	            KeyStore ks = KeyStore.getInstance("JKS");
	            String configDir = System.getProperty("jboss.server.config.dir");
	            String keystorePath = configDir + File.separator + "jwt2keystore.keystore";
	            fis = new FileInputStream(keystorePath);
	            ks.load(fis, password);
	            Key key = ks.getKey(alias, password);
	            if (key instanceof PrivateKey) {
	                pk = (PrivateKey) key;
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        } finally {
	            if (fis != null) {
	                try {
	                    fis.close();
	                } catch (IOException e) {}
	            }
	        }
	        privateKey = pk;
	    }

	    private static final PrivateKey privateKey;
	    private static final int TOKEN_VALIDITY = 14400;
	    private static final String CLAIM_ROLES = "groups";
	    private static final String ISSUER = "quickstart-jwt-issuer";
	    private static final String AUDIENCE = "jwt-audience";

	    public String createJwt(final String subject, final String role) throws Exception {
	        JWSSigner signer = new RSASSASigner(privateKey);
	        JsonObjectBuilder claimsBuilder = Json.createObjectBuilder()
	                .add("username", subject)
	                .add("iss", ISSUER)
	                .add("aud", AUDIENCE)
	                .add(CLAIM_ROLES,  role)
	                .add("exp", ((System.currentTimeMillis() / 1000) + TOKEN_VALIDITY));

	        JWSObject jwsObject = new JWSObject(new JWSHeader.Builder(JWSAlgorithm.RS256)
	                .type(new JOSEObjectType("jwt")).build(),
	                new Payload(claimsBuilder.build().toString()));

	        jwsObject.sign(signer);

	        return jwsObject.serialize();
	    }
}
