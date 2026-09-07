package com.jnu.ticketapi.api.slack.sender;

import static com.slack.api.model.block.Blocks.divider;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;

import com.jnu.ticketapi.api.event.event.DatabaseFallbackActivatedEvent;
import com.jnu.ticketinfrastructure.slack.SlackErrorNotificationProvider;
import com.slack.api.model.block.Blocks;
import com.slack.api.model.block.LayoutBlock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlackDatabaseFallbackSender {

    private final ObjectProvider<SlackErrorNotificationProvider> slackProvider;

    @TransactionalEventListener(
            classes = DatabaseFallbackActivatedEvent.class,
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void handle(DatabaseFallbackActivatedEvent event) {
        SlackErrorNotificationProvider provider = slackProvider.getIfAvailable();
        if (provider == null) {
            log.debug("Slack fallback notification is disabled. eventId: {}", event.eventId());
            return;
        }

        List<LayoutBlock> blocks =
                List.of(
                        Blocks.header(header -> header.text(plainText("Redis 장애: DB fallback 전환"))),
                        divider(),
                        section(
                                value ->
                                        value.fields(
                                                List.of(
                                                        markdownText(
                                                                "*Event ID*\n" + event.eventId()),
                                                        markdownText(
                                                                "*Admission Epoch*\n"
                                                                        + event
                                                                                .admissionEpoch())))),
                        section(value -> value.text(markdownText("*원인*\n" + event.cause()))),
                        section(
                                value ->
                                        value.text(
                                                markdownText(
                                                        "후속 신청은 로컬 상태 캐시를 통해 Redis를 건너뛰고 DB fallback으로 처리됩니다."))));
        provider.sendNotification(blocks);
    }
}
