package com.financas.party.api;

import com.financas.party.domain.Party;

public record PartyResponse(Long id, String name, String pixKey) {

    public static PartyResponse from(Party party) {
        return new PartyResponse(party.getId(), party.getName(), party.getPixKey());
    }
}
