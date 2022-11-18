package org.cda5.planning;


import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.wildfly.security.WildFlyElytronProvider;
import org.apache.sshd.common.config.keys.loader.openssh.kdf.BCrypt;
import org.cda5.planning.jwt.JwtManager;
import org.planning.entity.User;
import org.planning.entity.jwt;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.interfaces.BCryptPassword;
import org.wildfly.security.password.spec.EncryptablePasswordSpec;
import org.wildfly.security.password.spec.IteratedSaltedPasswordAlgorithmSpec;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import javax.servlet.http.Cookie;

@SuppressWarnings("deprecation")
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class jwttest {
	
	@PersistenceContext(unitName = "primary")
	private EntityManager em;
	
	static final Provider ELYTRON_PROVIDER = new WildFlyElytronProvider();
	
    private static final Logger log = Logger.getLogger(jwttest.class.getName());
    

    @Inject
    JwtManager jwtManager;
    
    @Context
    private SecurityContext securityContext;

    @POST
    @Path("/token")
    @Consumes("application/json")
    public Response postJWT(User entity) {
    	
    	String username = entity.getUsername();
    	String password = entity.getPassword();
    
    	TypedQuery<User> findByIdQuery = em
				.createQuery(
						"SELECT u  FROM User u where u.username = :username",
						User.class);
    	findByIdQuery
    	.setParameter("username", username);
    	
    	User result;
    	
    	result = findByIdQuery.getSingleResult();
    	String Cryptsalt = result.getSalt();
    	String Cryptpass = result.getPassword();
    	
    	Boolean CheckPassword = encryptPasswordCheck(Cryptsalt, Cryptpass, password);
    	
    	if(CheckPassword) {
    		try {
				String cookie = generateCookie(result.getUsername(), result.getRole());
				Cookie newcookie = new Cookie("token", cookie);
				return Response.ok(newcookie).build();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    	}else {
    		return Response.ok("test").build();
    	}
    	
    	return Response.status(Response.Status.UNAUTHORIZED).build();    
    }

    
    private  String generateCookie(String username, String role) throws Exception {
    	String token;
		
			token = jwtManager.createJwt(username, role);
			new jwt(token);
			Cookie cookie = new Cookie("token", token);
			cookie.setMaxAge(60 * 60 * 24 * 30);
				        
			return token;
    	
    }
    
    private Boolean encryptPasswordCheck(String saltQuery, String passwordQuery, String passwordEntity) {
    	String Cryptsalt = saltQuery;
    	String Cryptpass = passwordQuery;
    	PasswordFactory passwordFactory = null;
    	try {
    	passwordFactory = PasswordFactory.getInstance(BCryptPassword.ALGORITHM_BCRYPT, ELYTRON_PROVIDER);
    	} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	int iterationCount = 31;
    	byte[] decodedBytes = Base64.getDecoder().decode(Cryptsalt);
    
    	IteratedSaltedPasswordAlgorithmSpec iteratedAlgorithmSpec = new IteratedSaltedPasswordAlgorithmSpec(iterationCount, decodedBytes);
    	EncryptablePasswordSpec encryptableSpec = new EncryptablePasswordSpec(passwordEntity.toCharArray(), iteratedAlgorithmSpec);
    	BCryptPassword original = null;
    	try {
    		original = (BCryptPassword) passwordFactory.generatePassword(encryptableSpec);
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	byte[] hash = original.getHash();
    	Encoder encoder = Base64.getEncoder();
    	String VerifEncode = encoder.encodeToString(hash);
    	Boolean VerifPassword = VerifEncode.contentEquals(Cryptpass);
    	return VerifPassword;
    }
}
