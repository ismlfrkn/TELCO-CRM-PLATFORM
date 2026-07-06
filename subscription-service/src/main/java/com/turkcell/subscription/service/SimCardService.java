package com.turkcell.subscription.service;

import com.turkcell.subscription.dto.request.SimCardAssignRequest;
import com.turkcell.subscription.dto.request.SimCardRegisterRequest;
import com.turkcell.subscription.dto.response.SimCardResponse;
import com.turkcell.subscription.entity.SimCard;
import com.turkcell.subscription.exception.DuplicateSimCardException;
import com.turkcell.subscription.exception.SimCardNotFoundException;
import com.turkcell.subscription.mapper.SimCardMapper;
import com.turkcell.subscription.repository.SimCardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SimCardService {

    private final SimCardRepository simCardRepository;
    private final SimCardMapper simCardMapper;

    public SimCardService(SimCardRepository simCardRepository, SimCardMapper simCardMapper) {
        this.simCardRepository = simCardRepository;
        this.simCardMapper = simCardMapper;
    }

    @Transactional
    public SimCardResponse registerSimCard(SimCardRegisterRequest request) {
        if (simCardRepository.existsById(request.getIccid()) || simCardRepository.existsByImsi(request.getImsi())) {
            throw new DuplicateSimCardException("SIM card already registered with this iccid or imsi");
        }
        SimCard simCard = new SimCard(request.getIccid(), request.getImsi(), null, "UNASSIGNED");
        return simCardMapper.toResponse(simCardRepository.save(simCard));
    }

    public SimCardResponse getSimCardResponse(String iccid) {
        return simCardMapper.toResponse(getSimCard(iccid));
    }

    public SimCard getSimCard(String iccid) {
        return simCardRepository.findById(iccid)
                .orElseThrow(() -> new SimCardNotFoundException("SIM card not found with iccid: " + iccid));
    }

    @Transactional
    public SimCardResponse assignMsisdn(String iccid, SimCardAssignRequest request) {
        SimCard simCard = getSimCard(iccid);
        simCard.setMsisdn(request.getMsisdn());
        simCard.setStatus("ASSIGNED");
        return simCardMapper.toResponse(simCardRepository.save(simCard));
    }
}
