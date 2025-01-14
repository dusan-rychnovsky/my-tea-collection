package cz.dusanrychnovsky.myteacollection.util.upload;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InsertTeaToTeaTypeStatement implements AutoCloseable {

  private static final String SQL_QUERY =
    "INSERT INTO myteacollection.Teas_TeaTypes (tea_id, type_id) VALUES (?, ?)";

  private PreparedStatement stmt;

  public InsertTeaToTeaTypeStatement(Connection connection) throws SQLException {
    stmt = connection.prepareStatement(SQL_QUERY);
  }

  public void execute(Integer teaId, Integer typeId) throws SQLException {
    stmt.setInt(1, teaId);
    stmt.setInt(2, typeId);
    stmt.execute();
  }

  @Override
  public void close() throws SQLException {
    stmt.close();
  }
}
