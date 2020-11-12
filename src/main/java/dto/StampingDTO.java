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

import com.google.common.base.MoreObjects;

import org.joda.time.LocalDateTime;

/**
 * @author Daniele Murgia <dmurgia85@gmail.com>
 * @author Cristian Lucchesi <cristian.lucchesi@iit.cnr.it>
 *
 */
public class StampingDTO {

  /**
   * Orario della timbratura
   */
  public LocalDateTime DATA;

  /**
   * Verso lettore. E entrata U uscita
   */
  public String VERSO;

  public String CAUS;
  public String BADGE;
  public Long IDTIMBRATURA;

  public String LETTORE;
  
  @Override
  public String toString() {

    return MoreObjects.toStringHelper(this.getClass())
        .add("DATA", DATA)
        .add("VERSO", VERSO)
        .add("CAUS", CAUS)
        .add("BADGE", BADGE)
        .add("IDTIMBRATURA", IDTIMBRATURA)
        .add("LETTORE", LETTORE)
        .toString();
  }

}
