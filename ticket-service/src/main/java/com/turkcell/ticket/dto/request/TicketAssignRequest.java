package com.turkcell.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;

public class TicketAssignRequest {

    @NotBlank(message = "Team cannot be blank")
    private String team;

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }
}
