/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.server.internal;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.net.HostAndPort;
import ratpack.handling.Context;
import ratpack.http.Headers;
import ratpack.server.PublicAddress;
import ratpack.util.Exceptions;
import ratpack.util.internal.ProtocolUtil;

import java.net.URI;
import java.net.URISyntaxException;

import static ratpack.http.internal.HttpHeaderConstants.*;
import static ratpack.util.internal.ProtocolUtil.HTTPS_SCHEME;

public class InferringPublicAddress implements PublicAddress {

  private static final Splitter FORWARDED_HOST_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private final String scheme;

  public InferringPublicAddress(String scheme) {
    this.scheme = scheme;
  }

  public URI get(Context ctx) {
    String scheme;
    String host;
    int port;
    HostAndPort forwardedHostData = getForwardedHostData(ctx);
    if (forwardedHostData != null) {
      scheme = determineScheme(ctx, this.scheme);
      host = forwardedHostData.getHostText();
      port = forwardedHostData.getPortOrDefault(-1);
    } else {
      URI absoluteRequestURI = getAbsoluteRequestUri(ctx);
      if (absoluteRequestURI != null) {
        scheme = determineScheme(ctx, absoluteRequestURI.getScheme());
        host = absoluteRequestURI.getHost();
        port = absoluteRequestURI.getPort();
      } else {
        scheme = determineScheme(ctx, this.scheme);
        HostAndPort hostData = getHostData(ctx);
        if (hostData != null) {
          host = hostData.getHostText();
          port = hostData.getPortOrDefault(-1);
        } else {
          HostAndPort localAddress = ctx.getRequest().getLocalAddress();
          host = localAddress.getHostText();
          port = ProtocolUtil.isDefaultPortForScheme(localAddress.getPort(), this.scheme) ? -1 : localAddress.getPort();
        }
      }
    }
    try {
      return new URI(scheme, null, host, port, null, null, null);
    } catch (URISyntaxException ex) {
      throw Exceptions.uncheck(ex);
    }
  }

  private URI getAbsoluteRequestUri(Context context) {
    String rawUri = Strings.nullToEmpty(context.getRequest().getRawUri());
    if (rawUri.isEmpty() || rawUri.startsWith("/")) {
      return null;
    }
    return URI.create(rawUri);
  }

  private HostAndPort getForwardedHostData(Context context) {
    Headers headers = context.getRequest().getHeaders();
    String forwardedHostHeader = Strings.emptyToNull(headers.get(X_FORWARDED_HOST.toString()));
    String hostPortString = forwardedHostHeader != null ? Iterables.getFirst(FORWARDED_HOST_SPLITTER.split(forwardedHostHeader), null) : null;
    return hostPortString != null ? HostAndPort.fromString(hostPortString) : null;
  }

  private HostAndPort getHostData(Context context) {
    Headers headers = context.getRequest().getHeaders();
    String hostPortString = Strings.emptyToNull(headers.get(HOST.toString()));
    return hostPortString != null ? HostAndPort.fromString(hostPortString) : null;
  }

  private String determineScheme(Context context, String defaultScheme) {
    Headers headers = context.getRequest().getHeaders();
    String forwardedSsl = headers.get(X_FORWARDED_SSL.toString());
    String forwardedProto = headers.get(X_FORWARDED_PROTO.toString());
    if (ON.toString().equalsIgnoreCase(forwardedSsl)) {
      return HTTPS_SCHEME;
    }
    return forwardedProto != null ? forwardedProto : defaultScheme;
  }
}
