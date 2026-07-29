package com.financas.group.api;

import com.financas.group.domain.Group;
import com.financas.party.api.PartyResponse;
import java.util.Comparator;
import java.util.List;

public record GroupResponse(Long id, String name, List<PartyResponse> members) {

    public static GroupResponse from(Group group) {
        List<PartyResponse> members = group.getMembers().stream()
                .map(PartyResponse::from)
                .sorted(Comparator.comparing(PartyResponse::name))
                .toList();
        return new GroupResponse(group.getId(), group.getName(), members);
    }
}
