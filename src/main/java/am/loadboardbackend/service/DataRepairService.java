package am.loadboardbackend.service;

import am.loadboardbackend.model.Broker;
import am.loadboardbackend.model.Carrier;
import am.loadboardbackend.model.User;
import am.loadboardbackend.model.UserRole;
import am.loadboardbackend.repository.BrokerRepository;
import am.loadboardbackend.repository.CarrierRepository;
import am.loadboardbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Runs once at startup to repair any CARRIER or BROKER users that were
 * registered before the stub-creation fix was deployed (i.e. users whose
 * carrier_id / broker_id column is NULL).  Creates placeholder records so that
 * the admin panel always has a profileId to work with, and so the user can
 * later fill in real values via the profile endpoint.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataRepairService implements ApplicationListener<ApplicationReadyEvent> {

    private final UserRepository    userRepository;
    private final CarrierRepository carrierRepository;
    private final BrokerRepository  brokerRepository;

    @Override
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {
        repairCarrierUsers();
        repairBrokerUsers();
    }

    private void repairCarrierUsers() {
        List<User> orphans = userRepository.findByRoleAndCarrierIsNull(UserRole.ROLE_CARRIER);
        if (orphans.isEmpty()) return;

        log.warn("DataRepair: found {} CARRIER user(s) with no carrier record; creating stubs", orphans.size());
        for (User user : orphans) {
            String pendingDot = "PENDING-" + user.getId();
            String pendingMc  = "PENDING-MC-" + user.getId();

            Carrier carrier = carrierRepository.findByDotNumber(pendingDot)
                    .orElseGet(() -> {
                        Carrier c = new Carrier();
                        c.setDotNumber(pendingDot);
                        c.setMcNumber(pendingMc);
                        return carrierRepository.save(c);
                    });

            user.setCarrier(carrier);
            userRepository.save(user);
            log.info("DataRepair: linked stub carrier {} to user {}", carrier.getId(), user.getId());
        }
    }

    private void repairBrokerUsers() {
        List<User> orphans = userRepository.findByRoleAndBrokerIsNull(UserRole.ROLE_BROKER);
        if (orphans.isEmpty()) return;

        log.warn("DataRepair: found {} BROKER user(s) with no broker record; creating stubs", orphans.size());
        for (User user : orphans) {
            String pendingMc = "PENDING-MC-" + user.getId();

            Broker broker = brokerRepository.findByMcNumber(pendingMc)
                    .orElseGet(() -> {
                        Broker b = new Broker();
                        b.setMcNumber(pendingMc);
                        return brokerRepository.save(b);
                    });

            user.setBroker(broker);
            userRepository.save(user);
            log.info("DataRepair: linked stub broker {} to user {}", broker.getId(), user.getId());
        }
    }
}
