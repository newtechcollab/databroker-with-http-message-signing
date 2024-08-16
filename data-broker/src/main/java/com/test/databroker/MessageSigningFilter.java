package com.test.databroker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

@Component
public class MessageSigningFilter implements Filter {
	
	Logger logger = LoggerFactory.getLogger(MessageSigningFilter.class);

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		logger.info("Message signing filter -> START");
		HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpServletResponse responseCacheWrapperObject = new ContentCachingResponseWrapper(res);
        chain.doFilter(request, responseCacheWrapperObject);
        
        /*
        ** The following code signs the HTTP Response per IETF RFC 9421 standards
        */
        
        // Create a LinkedHashMap that stores all the headers that should be part of Signature String
        // The order in which the Headers are put in the Map is important, hence using LinkedHashMap
        LinkedHashMap<String, String> headers = new LinkedHashMap<String, String>();
        
        // For this example, we will use Request Method, Request Path and Content Digest
        
        // Get the HTTP Method being called
        String httpscheme = req.getMethod();
        
        // Get the Request URI
        String requestPath = req.getRequestURI();
        
        // Create the value of "@method";req header
        headers.put("\"@method\";req", httpscheme);
        
        // Create the value of "@path";req header
        headers.put("\"@path\";req", requestPath);
        
        // Get the HTTP Response Body and put its Digest as a Header
        String payload = null;
        ContentCachingResponseWrapper wrapper = WebUtils.getNativeResponse(responseCacheWrapperObject, ContentCachingResponseWrapper.class);
        if (wrapper != null) {
        	byte[] buffer = wrapper.getContentAsByteArray();
        	if (buffer.length > 0) {
        		payload = new String(buffer, wrapper.getCharacterEncoding());
        		logger.info("Original Response Body -> " + payload);
        		// Create Message Digest using Original Response Body
        		try {
					String messageDigest = this.generateMessageDigest(payload);
					// Put the HTTP Response Body in Signature String
					headers.put("content-digest", "SHA-256="+messageDigest);
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}        		
        	}
        }
        
        // Generate Signature String
        String signatureString = this.generateMessageSignatureString(headers);
        logger.info("Signature String is as follows");
        logger.info(signatureString);
        
        // Secret Key 
        String keyId = "abcd";
        
        // Generate HTTP Message Signature
        String messageSignature = null;
        try {
			messageSignature = this.generateSignature(signatureString, keyId);
			logger.info("Message Signature -> " + messageSignature);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        // Now, we will generate the values for Signature-Input and Signature header fields, per IETF RFC 9421
        StringBuffer signatureInput = new StringBuffer();
        
        signatureInput.append("reqres=");
        
        signatureInput.append("(");
        signatureInput.append("\"@method\";req");
        signatureInput.append(" ");
        signatureInput.append("\"@path\";req");
        signatureInput.append(" ");
        signatureInput.append("content-digest");
        signatureInput.append(")");
        
        signatureInput.append(";");
        
        signatureInput.append("keyId");
        signatureInput.append("=");
        signatureInput.append("\"");
        signatureInput.append(keyId);
        signatureInput.append("\"");
       
        signatureInput.append(";");
        
        signatureInput.append("alg");
        signatureInput.append("=");
        signatureInput.append("\"");
        signatureInput.append("hmac-sha256");
        signatureInput.append("\"");
        
        StringBuffer signatureBuffer = new StringBuffer();
        
        signatureBuffer.append("reqres=");
        signatureBuffer.append(messageSignature);
        
        // Write the HTTP Response in output stream
		res.setContentLength(payload.length());
		res.addHeader("Signature-Input", signatureInput.toString());
		res.addHeader("Signature", signatureBuffer.toString());
		res.getOutputStream().write(payload.getBytes());
        res.flushBuffer();
        
        logger.info("Message signing filter -> END");
	}
	
	// Generate Signature String
	private String generateMessageSignatureString(LinkedHashMap<String, String> headers) {
		
		StringBuffer concatenatedHeaderValues = new StringBuffer();
		int counter = 0;
		
		for (Map.Entry<String, String> header:headers.entrySet()) {
			
			// Get value of each Header
			logger.info("Header Name -> " + header.getKey() + " with Header Value -> " + header.getValue());
			concatenatedHeaderValues.append(header.getKey());
			concatenatedHeaderValues.append(":");
			concatenatedHeaderValues.append(" ");
			concatenatedHeaderValues.append(header.getValue());
			counter = counter + 1;
			if (counter == headers.size()) {
				logger.info("We have iterated through all the headers, so no need to append a newline");
			}
			else {
				concatenatedHeaderValues.append("\n");
			}
		}
		
		logger.info("Concatenated Headers -> " + concatenatedHeaderValues);
		return concatenatedHeaderValues.toString();
		
	}
	
	// Generate Message Digest
	private String generateMessageDigest(String messageBody) throws NoSuchAlgorithmException {
		
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] encodedMessageHash = digest.digest(messageBody.getBytes(StandardCharsets.UTF_8));
		byte[] base64EncodedMessageDigest = Base64.getEncoder().encode(encodedMessageHash);
		return new String(base64EncodedMessageDigest);
	}

	// Generate Signature
	private String generateSignature(String signatureString, String secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
		
		String algorithm = "HmacSHA256";
		SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), algorithm);
	    Mac mac = Mac.getInstance(algorithm);
	    mac.init(secretKeySpec);
	    byte[] signatureBytes = mac.doFinal(signatureString.getBytes());
		byte[] base64EncodedSignature = Base64.getEncoder().encode(signatureBytes);
		return new String(base64EncodedSignature);
	}
}
