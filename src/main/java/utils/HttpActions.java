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
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import dto.JsonStampingdto;
import java.util.List;
import org.apache.log4j.Logger;


/**
 * @author Daniele Murgia <dmurgia85@gmail.com>
 * @author Cristian Lucchesi <cristian.lucchesi@iit.cnr.it>
 * @since 01/03/16.
 */
public class HttpActions {

  private final Logger logger;

  public HttpActions(Logger logger) {
    this.logger = logger;
  }

  /**
   * Invia le timbrature ad ePAS.
   */
  public List<JsonStampingdto> sendStampings(final String url, final String user,
      final String password,
      final List<JsonStampingdto> stampingsDTO) {

    List<JsonStampingdto> stampingsInTrouble = Lists.newArrayList();
    logger.info(String.format("Invio di %d timbrature al server %s", stampingsDTO.size(), url));

    final Gson gson = new Gson();
    for (JsonStampingdto jDto : stampingsDTO) {
      try {
        String json = gson.toJson(jDto);

        int status = HttpUtils.postRequest(json, url, user, password);

        if (Client.RETRY_CODES.contains(status)) {
          logger.info(String.format("Risposta del server - %s. La timbratura " +
              "verrà archiviata per il re-invio: %s", status, jDto));
          stampingsInTrouble.add(jDto);
        } else {
          logger.info(String.format("Risposta del server - %s. " +
                  "Timbratura inviata correttamente: %s", status, jDto));
        }
      } catch (Exception e) {
        logger.warn(String.format("Problema d'invio della timbratura ad ePas: %s - %s",
            e.getMessage(), jDto));
        stampingsInTrouble.add(jDto);
      }
    }

    logger.info(String.format("Inviate correttamente a epas %s di %s timbrature",
        stampingsDTO.size() - stampingsInTrouble.size(), stampingsDTO.size()));

    return stampingsInTrouble;
  }

}
