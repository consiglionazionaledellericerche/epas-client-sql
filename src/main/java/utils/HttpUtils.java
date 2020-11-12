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
import java.io.IOException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;


/**
 * @author Daniele Murgia <dmurgia85@gmail.com>
 * @author Cristian Lucchesi <cristian.lucchesi@iit.cnr.it>
 *
 */
public class HttpUtils {

  /**
   * Connessione con basic auth.
   */
  private static CloseableHttpClient getClientConnection(String user, String password) {
    CredentialsProvider provider = new BasicCredentialsProvider();
    UsernamePasswordCredentials credentials =
        new UsernamePasswordCredentials(user, password);
    provider.setCredentials(AuthScope.ANY, credentials);

    final RequestConfig globalConfig = RequestConfig.custom()
        .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
        .setSocketTimeout(Client.CONNECTION_TIMEOUT * 1000)
        .setConnectTimeout(Client.CONNECTION_TIMEOUT * 1000)
        .build();

    final CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider)
        .setDefaultRequestConfig(globalConfig).build();

    return client;
  }

  public static int postRequest(String body, String url,
      String user, String password) throws IOException {

    CloseableHttpClient client = getClientConnection(user, password);
    HttpPost httpPost = new HttpPost(url);
    //Corpo
    StringEntity postingString = new StringEntity(body, "UTF-8");
    postingString.setContentType("application/json");
    httpPost.addHeader("content-type", "application/json");
    httpPost.setEntity(postingString);

    CloseableHttpResponse response = client.execute(httpPost);

    try {
      return response.getStatusLine().getStatusCode();
    } finally {
      // chiusura manuale della connessione
      response.close();
      client.close();
    }

  }

}
