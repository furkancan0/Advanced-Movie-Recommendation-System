package com.movieapp.util;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import com.pgvector.PGvector;
import org.hibernate.usertype.UserType;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class PGvectorType implements UserType<PGvector> {

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<PGvector> returnedClass() {
        return PGvector.class;
    }

    @Override
    public boolean equals(PGvector x, PGvector y) {
        if (x == y) return true;
        if (x == null || y == null) return false;
        return x.equals(y);
    }

    @Override
    public int hashCode(PGvector x) {
        return x != null ? x.hashCode() : 0;
    }

    @Override
    public PGvector nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        String value = rs.getString(position);
        return value != null ? new PGvector(value) : null;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, PGvector value, int index, SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            st.setObject(index, value);
        }
    }

    @Override
    public PGvector deepCopy(PGvector value) {
        if (value == null) return null;
        return new PGvector(value.toArray());
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(PGvector value) {
        return deepCopy(value);
    }

    @Override
    public PGvector assemble(Serializable cached, Object owner) {
        return deepCopy((PGvector) cached);
    }
}