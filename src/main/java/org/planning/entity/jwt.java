package org.planning.entity;

import javax.persistence.Entity;


public class jwt {
	   private String token;

	    public jwt(String token) {
	        this.token = token;
	    }

	    public void setToken(String token) {
	        this.token = token;
	    }

	    public String getToken() {
	        return token;
	    }
}
