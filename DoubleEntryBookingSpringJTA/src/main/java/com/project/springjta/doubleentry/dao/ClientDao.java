package com.project.springjta.doubleentry.dao;

import java.util.Date;


public interface ClientDao {
	
	void createClient(String clientRef, Date creationDate);
	
	void truncateTables();
}
