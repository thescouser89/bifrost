package org.jboss.pnc.bifrost.endpoint.websocket;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@Getter
@Setter
@NoArgsConstructor
public class SubscribeResultDto extends Result<String> {

    public SubscribeResultDto(String subscriptionTopic) {
        this.value = subscriptionTopic;
    }

}
