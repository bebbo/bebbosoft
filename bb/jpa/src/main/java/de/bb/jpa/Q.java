package de.bb.jpa;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.Parameter;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TemporalType;


class Q implements Query {

  private Class<?> clazz;
  private PreparedStatement ps;
  private int firstResult;
  private int maxResults = Integer.MAX_VALUE;

  Q(Class<?> clazz, PreparedStatement ps) {
    this.clazz = clazz;
    this.ps = ps;
  }

  @Override
  public int executeUpdate() {
    try {
      return ps.executeUpdate();
    } catch (SQLException e) {
      throw new PersistenceException(e);
    }
  }

  @Override
  public List getResultList() {
    try {
      ResultSet rs = ps.executeQuery();
      ArrayList<Object> res = new ArrayList<Object>();
      rs.relative(firstResult);
      int n = 0;
      while (rs.next() && n++ < maxResults) {
        Object o = clazz.newInstance();
        fill(o, rs);
        res.add(o);
      }
      rs.close();
      return res;
    } catch (Exception e) {
      throw new PersistenceException(e);
    }
  }

  private void fill(Object o, ResultSet rs) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Object getSingleResult() {
    try {
      ResultSet rs = ps.executeQuery();
      rs.relative(firstResult);
      if (!rs.next())
        throw new NoResultException();
      Object o = clazz.newInstance();
      fill(o, rs);
      rs.close();
      return o;
    } catch (Exception e) {
      throw new PersistenceException(e);
    }
  }

  @Override
  public Query setFirstResult(int firstResult) {
    this.firstResult = firstResult;    
    return this;
  }

  @Override
  public Query setFlushMode(FlushModeType arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Query setHint(String arg0, Object arg1) {
    // TODO Auto-generated method stub
    return this;
  }

  @Override
  public Query setMaxResults(int maxResults) {
    this.maxResults  = maxResults;
    return this;
  }

  @Override
  public Query setParameter(String arg0, Object arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Query setParameter(int arg0, Object arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Query setParameter(String arg0, Date arg1, TemporalType arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Query setParameter(String arg0, Calendar arg1, TemporalType arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Query setParameter(int arg0, Date arg1, TemporalType arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Query setParameter(int arg0, Calendar arg1, TemporalType arg2) {
    // TODO Auto-generated method stub
    return null;
  }

@Override
public int getFirstResult() {
	// TODO Auto-generated method stub
	return 0;
}

@Override
public FlushModeType getFlushMode() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Map<String, Object> getHints() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public LockModeType getLockMode() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public int getMaxResults() {
	// TODO Auto-generated method stub
	return 0;
}

@Override
public Parameter<?> getParameter(String arg0) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Parameter<?> getParameter(int arg0) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public <T> Parameter<T> getParameter(String arg0, Class<T> arg1) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public <T> Parameter<T> getParameter(int arg0, Class<T> arg1) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public <T> T getParameterValue(Parameter<T> arg0) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Object getParameterValue(String arg0) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Object getParameterValue(int arg0) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Set<Parameter<?>> getParameters() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public boolean isBound(Parameter<?> arg0) {
	// TODO Auto-generated method stub
	return false;
}

@Override
public Query setLockMode(LockModeType arg0) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public <T> Query setParameter(Parameter<T> arg0, T arg1) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Query setParameter(Parameter<Calendar> arg0, Calendar arg1, TemporalType arg2) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Query setParameter(Parameter<Date> arg0, Date arg1, TemporalType arg2) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public <T> T unwrap(Class<T> arg0) {
	// TODO Auto-generated method stub
	return null;
}

}
