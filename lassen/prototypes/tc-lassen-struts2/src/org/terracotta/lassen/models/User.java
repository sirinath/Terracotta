package org.terracotta.lassen.models;

import com.opensymphony.xwork2.validator.annotations.EmailValidator;
import com.opensymphony.xwork2.validator.annotations.RequiredStringValidator;
import com.opensymphony.xwork2.validator.annotations.StringLengthFieldValidator;
import com.opensymphony.xwork2.validator.annotations.Validation;
import com.opensymphony.xwork2.validator.annotations.ValidatorType;

@Validation
public class User {
	private int id;
	private String username;
	private String password;
	private String email;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@RequiredStringValidator(type = ValidatorType.FIELD, message = "Username is required.")
	@StringLengthFieldValidator(type = ValidatorType.FIELD, minLength = "5", maxLength = "8", message = "Username must be between ${minLength} and ${maxLength} characters.")
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@StringLengthFieldValidator(type = ValidatorType.FIELD, minLength = "5", maxLength = "8", message = "Password must be between ${minLength}  and ${maxLength} characters.")
	@RequiredStringValidator(type = ValidatorType.FIELD, message = "Password is required.")
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@RequiredStringValidator(type = ValidatorType.FIELD, message = "Email is required.")
	@EmailValidator(type = ValidatorType.FIELD, message = "Email is invalid.")
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}