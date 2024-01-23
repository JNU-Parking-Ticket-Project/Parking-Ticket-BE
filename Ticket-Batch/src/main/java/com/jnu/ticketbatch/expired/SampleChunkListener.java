package com.jnu.ticketbatch.expired;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.stereotype.Component;

@Component
public class SampleChunkListener implements ChunkListener {
    private static final Logger log = LoggerFactory.getLogger(SampleChunkListener.class);

    @Override
    public void beforeChunk(ChunkContext context) {
        StepContext stepContext = context.getStepContext();

        StepExecution stepExecution = stepContext.getStepExecution();
        log.info("###### beforeChunk : " + stepExecution.getReadCount());
    }

    @Override
    public void afterChunk(ChunkContext context) {
        StepContext stepContext = context.getStepContext();
        StepExecution stepExecution = stepContext.getStepExecution();
        log.info("##### afterChunk : " + stepExecution.getCommitCount());
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        StepContext stepContext = context.getStepContext();
        StepExecution stepExecution = stepContext.getStepExecution();
        log.info("##### afterChunkError : " + stepExecution.getRollbackCount());
    }
}
