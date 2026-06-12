package com.coffee.platform.queue.dto;

public record QueueSnapshotResponse(
        Long queueId, String label, int maxSize, int waitingCount
) {}
