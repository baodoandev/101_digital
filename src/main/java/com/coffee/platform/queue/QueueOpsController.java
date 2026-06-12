package com.coffee.platform.queue;

import com.coffee.platform.auth.AuthPrincipal;
import com.coffee.platform.queue.dto.QueueEntryResponse;
import com.coffee.platform.queue.dto.QueueSnapshotResponse;
import com.coffee.platform.queue.dto.ServeResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/queues")
public class QueueOpsController {

    private final QueueOpsService queueOpsService;

    public QueueOpsController(QueueOpsService queueOpsService) {
        this.queueOpsService = queueOpsService;
    }

    @GetMapping("/{id}")
    public QueueSnapshotResponse getSnapshot(@PathVariable Long id,
                                             @AuthenticationPrincipal AuthPrincipal principal) {
        return queueOpsService.getSnapshot(id, principal);
    }

    @GetMapping("/{id}/entries")
    public List<QueueEntryResponse> getEntries(@PathVariable Long id,
                                               @AuthenticationPrincipal AuthPrincipal principal) {
        return queueOpsService.getEntries(id, principal);
    }

    @PostMapping("/{id}/serve-next")
    public ServeResponse serveNext(@PathVariable Long id,
                                   @AuthenticationPrincipal AuthPrincipal principal) {
        return queueOpsService.serveNext(id, principal);
    }

    @PostMapping("/{id}/entries/{entryId}/serve")
    public ServeResponse serveSpecific(@PathVariable Long id,
                                       @PathVariable Long entryId,
                                       @AuthenticationPrincipal AuthPrincipal principal) {
        return queueOpsService.serveSpecific(id, entryId, principal);
    }
}
