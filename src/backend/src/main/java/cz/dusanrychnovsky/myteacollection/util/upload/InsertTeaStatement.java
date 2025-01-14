package cz.dusanrychnovsky.myteacollection.util.upload;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InsertTeaStatement implements AutoCloseable {

  private static final String SQL_QUERY =
    "INSERT INTO myteacollection.Teas " +
    "(id, title, name, description, vendor_id, url, origin, cultivar, season, elevation, brewing_instructions, in_stock) " +
    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private PreparedStatement stmt;

  public InsertTeaStatement(Connection connection) throws SQLException {
    stmt = connection.prepareStatement(SQL_QUERY);
  }

  public void execute(Tea tea) throws SQLException {
    stmt.setInt(1, tea.getId());
    stmt.setString(2, tea.getTitle());
    stmt.setString(3, tea.getName());
    stmt.setString(4, tea.getDescription());
    stmt.setInt(5, tea.getVendorId());
    stmt.setString(6, tea.getUrl());
    stmt.setString(7, tea.getOrigin());
    stmt.setString(8, tea.getCultivar());
    stmt.setString(9, tea.getSeason());
    stmt.setString(10, tea.getElevation());
    stmt.setString(11, tea.getBrewingInstructions());
    stmt.setBoolean(12, tea.isInStock());
    stmt.execute();
  }

  @Override
  public void close() throws SQLException {
    stmt.close();
  }
}
