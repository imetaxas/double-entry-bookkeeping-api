package com.project.springjta.doubleentry.dao;

import java.util.Date;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

/**
 * The Data Object Access support to abstract and encapsulate all access to the client table of the data source.
 *
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public class ClientDaoImpl extends JdbcDaoSupport implements ClientDao {

	@Override
	public void createClient(String clientRef, Date creationDate) {
		String sql = "INSERT INTO client (ref, creation_date) VALUES (?, ?)";
        getJdbcTemplate().update(sql, clientRef, creationDate);
	}
	
	@Override
    public void truncateTables() {
        getJdbcTemplate().execute("TRUNCATE TABLE client");
    }
}
