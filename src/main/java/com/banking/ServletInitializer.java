package com.banking;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Enables deployment of the banking application as a WAR to an external
 * servlet container (Tomcat 10.1+/Jakarta EE 10+).
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(BankingApplication.class);
    }
}
