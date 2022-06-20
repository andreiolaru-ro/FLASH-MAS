package net.xqhs.flash.http;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

public class HttpsCustomConfigurator extends HttpsConfigurator
{
	public HttpsCustomConfigurator(SSLContext sslContext) {
		super(sslContext);
	}
	
	@Override
	public void configure(HttpsParameters httpsParameters) {
		try {
			SSLContext context = getSSLContext();
			SSLEngine engine = context.createSSLEngine();
			httpsParameters.setNeedClientAuth(false);
			httpsParameters.setCipherSuites(engine.getEnabledCipherSuites());
			httpsParameters.setProtocols(engine.getEnabledProtocols());

			
			SSLParameters sslParameters = context.getSupportedSSLParameters();
			httpsParameters.setSSLParameters(sslParameters);

		} catch (Exception ex) {
			System.out.println("Failed to create HTTPS port");
		}
	}
}
