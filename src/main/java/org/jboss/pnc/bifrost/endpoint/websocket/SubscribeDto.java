package org.jboss.pnc.bifrost.endpoint.websocket;

import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@Getter
@Setter
public class SubscribeDto {

    private String matchFilters;

    private String prefixFilters;

}
