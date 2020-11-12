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

package dto;

import app.Client;
import com.google.common.base.MoreObjects;
import java.util.function.Function;
import org.apache.log4j.Logger;
import org.joda.time.LocalDateTime;

/**
 * @author Daniele Murgia <dmurgia85@gmail.com>
 * @author Cristian Lucchesi <cristian.lucchesi@iit.cnr.it>
 *
 */
public class JsonStampingdto {

  /**
   * Chiave mapping timbratura -> persona. "val(badgeNumber)"
   */
  public String matricolaFirma;

  public int anno;
  public int mese;
  public int giorno;
  public int giornoSettimana;
  public int ora;
  public int minuti;
  public int secondi;

  /**
   * Causale JSON to epas. es. motiviDiServizio
   */
  public String causale;

  public String lettore;
  public String tipo;

  /**
   * Verso JSON to epas. 0 in 1 out
   */
  public int operazione;

  public boolean admin;

  @Override
  public String toString() {

    return MoreObjects.toStringHelper(getClass())
        .add("matricolaFirma", matricolaFirma)
        .add("anno", anno)
        .add("mese", mese)
        .add("giorno", giorno)
        .add("giornoSettimana", giornoSettimana)
        .add("ora", ora)
        .add("minuti", minuti)
        .add("secondi", secondi)
        .add("causale", causale)
        .add("lettore", lettore)
        .add("tipo", tipo)
        .add("operazione", operazione)
        .add("admin", admin)
        .toString();
  }

  public enum stampingToDTO implements Function<StampingDTO, JsonStampingdto> {
    ISTANCE;

    private final Logger logger = Logger.getLogger(stampingToDTO.class);
    
    @Override
    public JsonStampingdto apply(StampingDTO sdto) {
      JsonStampingdto dto = new JsonStampingdto();

      // Il Badge è null quando le timbrature vengono inserite manualmente sul timeweb
      // La matricola invece dovrebbe essere sempre valorizzata
      dto.matricolaFirma = sdto.BADGE;

      final LocalDateTime dateTime;

      // DATAO è null quando le timbrature vengono inserite manualmente sul timeweb,
      // e la data della timbratura è quella in DATAV
      dateTime = sdto.DATA;
      dto.operazione = Client.DB_FIELDS_VERSO_ENTRATA.equals(sdto.VERSO) ? 0 : 1;

      dto.anno = dateTime.getYear();
      dto.mese = dateTime.getMonthOfYear();
      dto.giorno = dateTime.getDayOfMonth();
      dto.giornoSettimana = dateTime.getDayOfWeek();
      dto.ora = dateTime.getHourOfDay();
      dto.minuti = dateTime.getMinuteOfHour();
      dto.secondi = dateTime.getSecondOfMinute();

      if (sdto.CAUS != null) {
        if (sdto.CAUS.equalsIgnoreCase(Client.DB_FIELDS_CAUSALE_MOTIVI_DI_SERVIZIO)) {
          dto.causale = "motiviDiServizio";
        } else if (sdto.CAUS.equalsIgnoreCase(Client.DB_FIELDS_CAUSALE_PAUSA_PRANZO)) {
          dto.causale = "pausaPranzo";
        } else {
          logger.debug(String.format("Timbrature id = %s, causale %s scononosciuta quindi ignorata.",
              sdto.IDTIMBRATURA, sdto.CAUS));
        }
      }

      dto.lettore = sdto.LETTORE;
      return dto;
    }
  }
}
