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

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dto.JsonStampingdto;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * @author Daniele Murgia <dmurgia85@gmail.com>
 * @author Cristian Lucchesi <cristian.lucchesi@iit.cnr.it>
 * @since 29/02/16.
 */
public class FileUtils {

  public static final String LAST_STAMPING_ID = "last_stamping_id";
  public static final String STAMPS_IN_TROUBLE_FILE = "stampsInTrouble.json";
  private static final String LAST_REQUEST_FILE = "lastRequest.txt";
  private static final String DATA_FOLDER = "data";

  private final Logger logger;

  public FileUtils(final Logger logger) {
    this.logger = logger;
  }

  /**
   * Salva sul file le timbrature/assenze con problemi.
   */
  public void saveInFile(final List<JsonStampingdto> list, final String file, boolean append) {

    if (list.isEmpty()) {
      return;
    }

    try {
      final String filePath = DATA_FOLDER + "/" + file;
      final File stampIntrouble = new File(filePath);

      final Gson gson = new GsonBuilder().setPrettyPrinting().create();
      
      if (stampIntrouble.exists()) {
        if (append) {
          BufferedReader br = new BufferedReader(new FileReader(filePath));
          
          //Leggo le timbrature attuali nel file per poterle ri-serializzare correttamente.
          List<JsonStampingdto> loadedStampings = gson.fromJson(br,
              new TypeToken<List<JsonStampingdto>>() {
          }.getType());
          logger.info(String.format("Lette %s timbrature dal file %s", loadedStampings.size(), filePath));

          list.addAll(loadedStampings);
        } else {
          if (stampIntrouble.delete()) {
            logger.trace("Eliminato file " + stampIntrouble.getName());
          } else {
            logger.warn("Impossibile eliminare file " + stampIntrouble.getName());
          }
        }                
        
      } else {
        logger.info(String.format("Il file %s non esiste, creato un file vuoto.", file));
        stampIntrouble.createNewFile();        
      }
      
      final InputStream is = new ByteArrayInputStream(gson.toJson(list).getBytes());
      ByteStreams.copy(is, new FileOutputStream(stampIntrouble, false));
      logger.info(String.format("Salvate %s record sul file %s", list.size(), stampIntrouble.getName()));

    } catch (IOException e) {
      logger.warn(String.format("Errore durante il salvataggio nel file %s: %s",
          file, e.getMessage()));
    }
  }

  /**
   * @return L'id dell'ultima timbratura prelevata dal db nella richiesta precedente, null
   * altrimenti
   */
  public Long loadValue(final String field) {

    try {
      Properties properties = new Properties();

      final String lastRequestPath = DATA_FOLDER + "/" + LAST_REQUEST_FILE;

      File lastRequestFile = new File(lastRequestPath);

      if (lastRequestFile.exists()) {
        properties.load(new FileInputStream(lastRequestPath));
      }
      final String value = properties.getProperty(field);

      if (Strings.isNullOrEmpty(value)) {
        logger.warn(String.format("Parametro %s nel file %s non valido",
            lastRequestPath, value));
        return null;
      }
      logger.info(String.format("Caricato dal file %s %s=%s",
          LAST_REQUEST_FILE, field, value));

      return Long.parseLong(value);
    } catch (IOException e) {
      logger.warn(String.format("Errore nel caricamento delle informazioni dal file %s: %s",
          LAST_REQUEST_FILE, e.getMessage()));
      return null;
    }
  }

  /**
   * Salva nel file la data passata come parametro
   *
   * @param field field Name to save
   * @param value field value to save
   */
  public void saveValue(final String field, final Long value) {

    try {
      Properties properties = new Properties();

      final String lastRequestPath = DATA_FOLDER + "/" + LAST_REQUEST_FILE;

      File lastRequestFile = new File(lastRequestPath);

      if (lastRequestFile.exists()) {
        properties.load(new FileInputStream(lastRequestPath));
      }

      properties.setProperty(field, value.toString());

      properties.store(new FileOutputStream(lastRequestPath), "Valori ultime richieste");
      logger.info(String.format("Salvato nel file %s il parametro %s=%s", LAST_REQUEST_FILE, field, value));
    } catch (IOException e) {
      logger.error(String.format("Errore durante il salvataggio delle informazioni sul file %s:%s",
          LAST_REQUEST_FILE, e.getMessage()));
    }
  }

  public List<JsonStampingdto> loadBadStampings() {

    List<JsonStampingdto> stampingsDTO = Lists.newArrayList();

    try {
      final String filePath = DATA_FOLDER + "/" + STAMPS_IN_TROUBLE_FILE;
      BufferedReader br = new BufferedReader(new FileReader(filePath));
      Gson gson = new Gson();

      List<JsonStampingdto> loadedStampings = gson.fromJson(br,
          new TypeToken<List<JsonStampingdto>>() {
      }.getType());

      if (loadedStampings != null) {        
        stampingsDTO.addAll(loadedStampings);
      }

      logger.info(String.format("Caricate %s stampings in trouble dal file %s",
          stampingsDTO.size(), filePath));

    } catch (FileNotFoundException e) {
      logger.warn("Errore nel recupero delle stampings in trouble: " + e.getMessage());
      return Lists.newArrayList();
    }
    return stampingsDTO;
  }

  public void checkFolders() {

    //  Verifica esistenza cartelle necessarie
    File data = new File(DATA_FOLDER);

    if (!data.exists()) {
      data.mkdirs();
    }
  }

}
