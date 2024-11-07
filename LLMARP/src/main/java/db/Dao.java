package db;

import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface Dao {
    @SqlQuery("INSERT INTO repositories (url) VALUES (?) RETURNING id")
    long insertRepository(final String url);
}
