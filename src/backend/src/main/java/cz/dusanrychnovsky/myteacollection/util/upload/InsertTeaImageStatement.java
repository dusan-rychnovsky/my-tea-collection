package cz.dusanrychnovsky.myteacollection.util.upload;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InsertTeaImageStatement implements AutoCloseable {

  private static final String SQL_QUERY =
    "INSERT INTO myteacollection.TeaImages (tea_id, index, data)" +
    "VALUES (?, ?, ?)";

  private PreparedStatement stmt;

  public InsertTeaImageStatement(Connection connection) throws SQLException {
    stmt = connection.prepareStatement(SQL_QUERY);
  }

  public void execute(Integer teaId, int idx, byte[] bytes) throws SQLException {
    stmt.setInt(1, teaId);
    stmt.setInt(2, idx);
    stmt.setBytes(3, bytes);
    stmt.executeUpdate();
  }

  @Override
  public void close() throws SQLException {
    stmt.close();
  }
}
