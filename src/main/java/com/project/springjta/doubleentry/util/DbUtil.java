package com.project.springjta.doubleentry.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

/**
* Utility class for the data base operations.
 *
 * @author yanimetaxas
 * @since 14-Nov-14
*/
public class DbUtil {
	
	private static class StringRowMapper implements RowMapper<String> {
		@Override
		public String mapRow(ResultSet rs, int rowNum) throws SQLException {
			return rs.getString(1);
		}
	}
	
	public RowMapper<Long> longRowMapper() {
		return new LongRowMapper();
	}
	
	public RowMapper<String> stringRowMapper() {
		return new StringRowMapper();
	}
	
	private static class LongRowMapper implements RowMapper<Long> {
		@Override
		public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
			return rs.getLong(1);
		}
	}
	
	public String commaSeparatedQuestionMarks(int count) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i<count; i++) {
			sb.append("?,");
		}
		String s = sb.toString();
		if (s.isEmpty()) {
			return "";
		}
		return s.substring(0, s.length() - 1);
	}
}

