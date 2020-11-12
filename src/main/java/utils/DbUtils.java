/*
 * Copyright (C) 2020  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package utils;

import app.Client;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import dto.StampingDTO;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import org.apache.log4j.Logger;
import org.joda.time.LocalDateTime;

/**
 * @author Daniele Murgia <dmurgia85@gmail.com>
 * @author Cristian Lucchesi <cristian.lucchesi@iit.cnr.it>
 * @since 29/02/16.
 */
public class DbUtils {

  private final Logger logger;

  public DbUtils(Logger logger) {
    this.logger = logger;
  }

  
  /**
   * @param serverName ServerName
   * @param port       Port for connection
   * @param dbName     Database Name
   * @param dbUser     User for authentication
   * @param dbPassword Password for authentication
   * @return a SQL Database Connection.
   * @throws SQLException if connection fail
   * @throws IllegalAccessException 
   * @throws InstantiationException 
   */
  public Connection getDBConnection(DbType dbType, final String serverName, 
      final int port, final String dbName,
      final String dbUser, final String dbPassword) throws SQLException, InstantiationException, IllegalAccessException {

    // Declare the JDBC objects.
    Connection connection = null;

    // Create a variable for the connection string.
    final StringBuilder connectionUrl = new StringBuilder();
    switch (dbType) {
      case mssql:
        connectionUrl.append(String.format("jdbc:sqlserver://%s:%s;", serverName, port));
        if (!Strings.isNullOrEmpty(dbName)) {
          connectionUrl.append(String.format("databaseName=%s;", dbName));
        }
        break;
      case mysql:
        try {
          Class.forName("com.mysql.jdbc.Driver").newInstance();
          } catch (ClassNotFoundException e) {
              throw new IllegalStateException("Cannot find the driver in the classpath!", e);
          }        
        connectionUrl.append(String.format("jdbc:mysql://%s:%s/%s", serverName, port, dbName));
        break;
      default:
        break;
    }

    try {
      // Establish the connection.
      logger.info(String.format("Tentativo di connessione al db: %s:%s", serverName, port));
      connection = DriverManager.getConnection(connectionUrl.toString(), dbUser, dbPassword);

      final DatabaseMetaData dbData = connection.getMetaData();

      logger.info(String.format("Connessione a %s avvenuta con successo con %s.",
          dbData.getDatabaseProductName(), dbData.getDriverName()));

      logger.info(String.format("URL Completo:  %s", dbData.getURL()));
    }
    // Handle any errors that may have occurred.
    catch (Exception e) {
      e.printStackTrace();
    }

    return connection;
  }

  /**
   * Preleva le timbrature dal DataBase a partire dall'id specificato.
   */
  public List<StampingDTO> getStampings(final Connection dbConnection, String table,
      final Long id) throws SQLException {

    PreparedStatement preparedStatement;
    String query;
    if (id == null) {
      // Se il campo id è null prelevo tutte le timbrature in base al parametro DAYS_BEFORE in configurazione      
      query = String.format("SELECT * FROM %s WHERE %s > ?", table, Client.DB_FIELDS_DATAORA.orElse(Client.DB_FIELDS_DATA.get()));
      preparedStatement = dbConnection.prepareStatement(query);
      java.sql.Timestamp date = new Timestamp(
          LocalDateTime.now().minusDays(Client.DAYS_BEFORE).toDateTime().getMillis());
      logger.info(String.format("Prelevo le timbrature a partire dal %s", date));
      preparedStatement.setTimestamp(1, date);
      logger.info("query = " + query);
    } else {
      query = String.format("SELECT * FROM %s WHERE %s > ?", table, Client.DB_FIELDS_ID_TIMBRATURA);
      preparedStatement = dbConnection.prepareStatement(query);
      preparedStatement.setLong(1, id);
    }

    List<StampingDTO> stampings = Lists.newArrayList();

    try {
      // execute select SQL statement
      ResultSet rs = preparedStatement.executeQuery();

      while (rs.next()) {
        
        //Build Timbratura
        final StampingDTO stamp = new StampingDTO();
        
        stamp.IDTIMBRATURA = rs.getLong(Client.DB_FIELDS_ID_TIMBRATURA);
        if (Client.DB_FIELDS_DATAORA.isPresent()) {
          
          final Timestamp data = rs.getTimestamp(Client.DB_FIELDS_DATAORA.get());          
          if (data != null) {
            stamp.DATA = LocalDateTime.fromDateFields(data);
          } else {
            //Ci sono alcune timbrature che non hanno la data valorizzata
            logger.warn(String.format(
                "Ignorara timbratura con id = %s perché ha data null", stamp.IDTIMBRATURA));
            continue;
          }
        } else {
          final Date date = rs.getDate(Client.DB_FIELDS_DATA.get());
          final Time hour = rs.getTime(Client.DB_FIELDS_ORA.get());
         
          //Ci sono alcune timbrature che non hanno la data valorizzata
          if (date == null || hour == null) {
            logger.warn(String.format(
                "Ignorara timbratura con id = %s perché ha data o ora null", stamp.IDTIMBRATURA));
            continue;            
          }
          
          logger.debug(String.format("Timbratura id=%s -> data = %s, ora = %s.", 
              stamp.IDTIMBRATURA, date, hour));
          
          // Trasformazione dei due campi data e ora in un unico LocalDateTime
          date.setTime(date.getTime() + hour.getTime());
          Calendar cal = Calendar.getInstance();
          cal.setTime(hour);
          stamp.DATA = new LocalDateTime(date)
              .withTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), 0);          
        }        

        stamp.VERSO = rs.getString(Client.DB_FIELDS_VERSO);
        stamp.BADGE = rs.getString(Client.DB_FIELDS_BADGE);
        
        // Mapping ed impostazione della causale in caso sia motivi di servizio o pausa pranzo.
        stamp.CAUS = rs.getString(Client.DB_FIELDS_CAUSALE);

        if (Client.DB_FIELDS_LETTORE != null && !Client.DB_FIELDS_LETTORE.isEmpty()) {
          stamp.LETTORE = rs.getString(Client.DB_FIELDS_LETTORE);
        }
        logger.debug(String.format("Prelevata timbratura dal db: %s", stamp));
        stampings.add(stamp);
      }
    } catch (SQLException e) {
      logger.error("Errore durante il recupero delle timbrature dal DataBase: "
          + e.getMessage());
    } finally {
      if (preparedStatement != null) {
        preparedStatement.close();
      }
      if (dbConnection != null) {
        dbConnection.close();
      }
    }

    logger.info(String.format("Recuperate %s timbrature dal db", stampings.size()));
    return stampings;
  }

}
