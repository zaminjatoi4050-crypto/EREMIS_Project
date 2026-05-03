package com.eremis.dao.interfaces;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Generic DAO contract that all entity-specific DAOs implement.
 *
 * @param <T>  the entity type
 * @param <ID> the primary-key type (usually Integer)
 */
public interface GenericDAO<T, ID> {

    /**
     * Persist a new entity and return it with its generated ID set.
     */
    T create(T entity) throws SQLException;

    /**
     * Find a single entity by its primary key.
     * Returns Optional.empty() if not found.
     */
    Optional<T> findById(ID id) throws SQLException;

    /**
     * Return every row in the table (use sparingly for large tables).
     */
    List<T> findAll() throws SQLException;

    /**
     * Persist changes to an existing entity.
     * Returns true if at least one row was affected.
     */
    boolean update(T entity) throws SQLException;

    /**
     * Hard-delete the entity by its primary key.
     * Returns true if at least one row was deleted.
     */
    boolean delete(ID id) throws SQLException;

    /**
     * Return the total count of rows.
     */
    int count() throws SQLException;
}
