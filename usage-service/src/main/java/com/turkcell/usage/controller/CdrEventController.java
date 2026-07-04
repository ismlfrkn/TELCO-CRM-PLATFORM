package com.turkcell.usage.controller;

import com.turkcell.usage.dto.request.CdrEventIngestRequest;
import com.turkcell.usage.dto.response.CdrEventResponse;
import com.turkcell.usage.service.CdrEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cdr-events")
public class CdrEventController {

    private final CdrEventService cdrEventService;

    public CdrEventController(CdrEventService cdrEventService) {
        this.cdrEventService = cdrEventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CdrEventResponse ingest(@Valid @RequestBody CdrEventIngestRequest request) {
        return cdrEventService.ingest(request);
    }
}
