package com.jnu.ticketinfrastructure.slack;


import com.jnu.ticketcommon.annotation.Helper;
import com.jnu.ticketinfrastructure.slack.config.SlackProperties;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.webhook.Payload;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.StringUtils;

@Helper
@RequiredArgsConstructor
@Slf4j
public class SlackHelper {
    private final MethodsClient methodsClient;

    private final SlackProperties slackProperties;

    public void sendNotification(String url, List<LayoutBlock> layoutBlocks) {
        try {
            doSend(url, layoutBlocks);
        } catch (Exception ignored) {
            log.error("SlackMessageProvider.sendNotification failed : ", ignored);
        }
    }

    private void doSend(String url, List<LayoutBlock> layoutBlocks)
            throws IOException, SlackApiException {
        final Slack slack = Slack.getInstance();
        final Payload payload =
                Payload.builder()
                        .blocks(layoutBlocks)
                        .username(slackProperties.getUsername())
                        .iconUrl(slackProperties.getIconUrl())
                        .build();
        try {
            String responseBody = slack.send(url, payload).getBody();
            if (!StringUtils.equals(responseBody, "ok")) {
                throw new IOException("Failed to send message to Slack URL: " + url);
            }
        } catch (IOException e) {
            log.error("Error occurred while sending message to Slack URL: " + url, e);
            throw e;
        }
    }
}
