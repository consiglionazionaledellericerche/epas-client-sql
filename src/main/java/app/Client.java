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

package app;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dto.JsonStampingdto;
import dto.StampingDTO;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.joda.time.LocalDate;
import utils.DbType;
import utils.DbUtils;
import utils.FileUtils;
import utils.HttpActions;


/**
 * Classe principale per chiamare attivare la lettura delle timbrature da SQL
 * ed loro invio ad ePAS.
 * 
 * @author Daniele Murgia <dmurgia85@gmail.com>
 * @author Cristian Lucchesi <cristian.lucchesi@iit.cnr.it>
 *
 */
public final class Client {

  private static final String CONNECTION_PROPS_FILE = "conf/connection.properties";
  private static final String DEFAULT_PORT = "1433";
  public static Set<Integer> RETRY_CODES = Sets.newHashSet(401, 404, 500, 501, 502, 503, 504, 505, 506, 507, 508, 509);

  public static int CONNECTION_TIMEOUT; // in seconds
  public static int DAYS_BEFORE;
  public static int MAX_BAD_STAMPING_DAYS;

  public static DbType DB_TYPE;
  private static String DB_HOST;
  private static int DB_PORT;
  private static String DB_NAME;
  private static String DB_USER;
  private static String DB_PASSWORD;
  private static String STAMPINGS_TABLE;
  
  public static String DB_FIELDS_ID_TIMBRATURA;
  public static String DB_FIELDS_BADGE;
  public static String DB_FIELDS_VERSO;
  public static String DB_FIELDS_VERSO_ENTRATA;
  
  public static Optional<String> DB_FIELDS_DATAORA;
  public static Optional<String> DB_FIELDS_DATA;
  public static Optional<String> DB_FIELDS_ORA;
  
  public static String DB_FIELDS_CAUSALE;
  public static String DB_FIELDS_LETTORE;
  
  public static String DB_FIELDS_CAUSALE_MOTIVI_DI_SERVIZIO;
  public static String DB_FIELDS_CAUSALE_PAUSA_PRANZO;
  
  private static String EPAS_STAMPINGS_URL;
  private static String EPAS_USER;
  private static String EPAS_PASSWORD;

  private Client() {
  }

  /**
   * Main method
   *
   * @param argv main arguments
   * @throws IllegalAccessException 
   * @throws InstantiationException 
   */
  public static void main(String... argv) throws InstantiationException, IllegalAccessException {
    if (argv.length > 0 && "-test".equals(argv[0])) {
      connectionTest();
    } else if (argv.length > 0 && "-badStampings".equalsIgnoreCase(argv[0])) {
      badStampings();
    } else {
      stampings();
    }
    System.exit(0);
  }

  private static void importConf() throws IOException {

    Properties properties = new Properties();
    properties.load(new FileInputStream(CONNECTION_PROPS_FILE));

    CONNECTION_TIMEOUT = Integer.parseInt(properties.getProperty("connection.timeout", "60"));
    DAYS_BEFORE = Integer.parseInt(properties.getProperty("days.before", "30"));
    MAX_BAD_STAMPING_DAYS = Integer.parseInt(properties.getProperty("badStamping.days", "5"));

    final String extraResponseCodes = properties.getProperty("retry.codes");

    if (extraResponseCodes != null) {
      for (String code : Splitter.on(",").omitEmptyStrings().trimResults()
          .split(extraResponseCodes)) {
        RETRY_CODES.add(Integer.parseInt(code));
      }
    }

    DB_TYPE = DbType.valueOf(properties.getProperty("db.type", "mssql"));
    DB_HOST = properties.getProperty("db.host");
    DB_PORT = Integer.parseInt(properties.getProperty("db.port", DEFAULT_PORT));
    DB_NAME = properties.getProperty("db.name");
    DB_USER = properties.getProperty("db.user");
    DB_PASSWORD = properties.getProperty("db.password");
    STAMPINGS_TABLE = properties.getProperty("stampings.table");
    
    DB_FIELDS_ID_TIMBRATURA = properties.getProperty("db.fields.idTimbratura", "IDTIMBRATURA");
    DB_FIELDS_BADGE = properties.getProperty("db.fields.badge", "BADGE");
    DB_FIELDS_VERSO = properties.getProperty("db.fields.verso", "VERSOO");
    DB_FIELDS_VERSO_ENTRATA = properties.getProperty("db.fields.verso.entrata", "E");
    DB_FIELDS_DATAORA= Optional.ofNullable(properties.getProperty("db.fields.dataora"));
    DB_FIELDS_DATA= Optional.ofNullable(properties.getProperty("db.fields.data"));
    DB_FIELDS_ORA= Optional.ofNullable(properties.getProperty("db.fields.ora"));
    DB_FIELDS_CAUSALE= properties.getProperty("db.fields.causale", "CAUS");
    DB_FIELDS_CAUSALE_MOTIVI_DI_SERVIZIO= properties.getProperty("db.fields.causale.motiviDiServizio", "motiviDiServizio");
    DB_FIELDS_CAUSALE_PAUSA_PRANZO= properties.getProperty("db.fields.causale.pausaPranzo", "pausaPranzo");
    
    //Non è obbligatorio, potrebbe essere NULL
    DB_FIELDS_LETTORE = properties.getProperty("db.fields.lettore");
    
    EPAS_STAMPINGS_URL = properties.getProperty("epas.stampings.url");
    EPAS_USER = properties.getProperty("epas.user");
    EPAS_PASSWORD = properties.getProperty("epas.password");
  }

  private static void connectionTest() throws InstantiationException, IllegalAccessException {
    final Logger testLogger = Logger.getLogger("testLogger");

    new FileUtils(testLogger).checkFolders();

    testLogger.info("########################################### AVVIO TEST DI CONNESSIONE "
        + "###########################################\n");

    // Load configurations
    try {
      importConf();
      testLogger.debug("Configurazione caricata dal file: " + CONNECTION_PROPS_FILE);
    } catch (Exception e) {
      testLogger.error("Impossibile caricare la configurazione da file: " + e.getMessage());
      return;
    }

    final DbUtils dbUtils = new DbUtils(testLogger);

    Connection connection = null;
    try {
      connection = dbUtils.getDBConnection(DB_TYPE, DB_HOST, DB_PORT,
          DB_NAME, DB_USER, DB_PASSWORD);
      connection.close();
    } catch (SQLException e) {
      testLogger.error("Errore connessione al db: " + e.getMessage());
    } finally {
      if (connection != null) {
        try {
          connection.close();
        } catch (Exception e) {
          testLogger.error("Errore durante la connessione al db:" + e.getMessage());
        }
      }
    }
    testLogger.info("######################################## TEST DI CONNESSIONE TERMINATO "
        + "########################################\n");
  }

  private static void stampings() throws InstantiationException, IllegalAccessException {

    final Logger stampingsLogger = Logger.getLogger("stampingsLogger");
    final DbUtils dbUtils = new DbUtils(stampingsLogger);
    final FileUtils fileUtils = new FileUtils(stampingsLogger);
    final HttpActions http = new HttpActions(stampingsLogger);

    stampingsLogger.info("########################################### AVVIO CLIENT TIMBRATURE "
        + "###########################################\n");

    fileUtils.checkFolders();
    // Load configurations
    try {
      importConf();
      stampingsLogger.debug("Configurazione caricata dal file: " + CONNECTION_PROPS_FILE);
    } catch (Exception e) {
      stampingsLogger.error("Impossibile caricare la configurazione da file: " + e.getMessage());
      return;
    }

    final Long lastId = fileUtils.loadValue(FileUtils.LAST_STAMPING_ID);
    final List<JsonStampingdto> stampingsDto = Lists.newArrayList();

    try {
      Connection dbConnection = dbUtils.getDBConnection(DB_TYPE, DB_HOST, DB_PORT,
          DB_NAME, DB_USER, DB_PASSWORD);

      List<StampingDTO> stampings = dbUtils
          .getStampings(dbConnection, STAMPINGS_TABLE, lastId);
      // Se ci sono timbrature prelevate dal db le trasformo in dto da inviare a ePas
      if (!stampings.isEmpty()) {

        // Prendo l'id con valore più alto dalla lista
        final Long maxId = stampings.stream().mapToLong(stampingDTO -> stampingDTO.IDTIMBRATURA)
            .max().getAsLong();

        fileUtils.saveValue(FileUtils.LAST_STAMPING_ID, maxId);

        stampingsDto.addAll(stampings.stream()
            .map(JsonStampingdto.stampingToDTO.ISTANCE::apply).collect(Collectors.toList()));
      }

    } catch (SQLException e) {
      stampingsLogger.error("Errore connessione al db: " + e.getMessage());
      return;
    }

    final List<JsonStampingdto> stampingsInTrouble = http.sendStampings(EPAS_STAMPINGS_URL,
        EPAS_USER, EPAS_PASSWORD, stampingsDto);

    fileUtils.saveInFile(stampingsInTrouble, FileUtils.STAMPS_IN_TROUBLE_FILE, true);

    stampingsLogger.info("####################################### ESECUZIONE CLIENT TIMBRATURE TERMINATA "
        + "#######################################\n");
  }

  private static void badStampings() {

    final Logger stampingsLogger = Logger.getLogger("stampingsLogger");
    final FileUtils fileUtils = new FileUtils(stampingsLogger);
    final HttpActions http = new HttpActions(stampingsLogger);

    stampingsLogger.info("################ AVVIO CLIENT TIMBRATURE PER L'INVIO DELLE " +
        "TIMBRATURE CON PROBLEMI #######################\n");

    fileUtils.checkFolders();
    // Load configurations
    try {
      importConf();
      stampingsLogger.debug("Configurazione caricata dal file: " + CONNECTION_PROPS_FILE);
    } catch (Exception e) {
      stampingsLogger.error("Impossibile caricare la configurazione da file: " + e.getMessage());
      return;
    }

    final LocalDate oldestDay = LocalDate.now().minusDays(MAX_BAD_STAMPING_DAYS);

    // Filtro le timbrature con problemi in modo che vengano scartate quelle più vecchie di X giorni
    final List<JsonStampingdto> stampingsDto = fileUtils.loadBadStampings().stream()
        .filter(stamp -> !oldestDay.isAfter(new LocalDate(stamp.anno, stamp.mese, stamp.giorno)))
        .collect(Collectors.toList());
    
    final List<JsonStampingdto> stampingsInTrouble = http.sendStampings(EPAS_STAMPINGS_URL,
        EPAS_USER, EPAS_PASSWORD, stampingsDto);

    fileUtils.saveInFile(stampingsInTrouble, FileUtils.STAMPS_IN_TROUBLE_FILE, false);

    stampingsLogger.info("####################################### ESECUZIONE CLIENT TIMBRATURE TERMINATA "
        + "#######################################\n");

  }

}
