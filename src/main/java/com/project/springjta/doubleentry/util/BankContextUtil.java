package com.project.springjta.doubleentry.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
* Utility class for getting the beans from the application context
*/
public class BankContextUtil {
	private static ApplicationContext context;
	
	private static void init() {
		context = new ClassPathXmlApplicationContext("applicationContext.xml");
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getBean(String beanName) {
		if (context == null) {
			init();
		}
		return (T) context.getBean(beanName);
	}
}