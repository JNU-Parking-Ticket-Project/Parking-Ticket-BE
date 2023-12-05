package com.jnu.ticketdomain.domains.announce.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketcommon.exception.AnnounceIdNotExistException;
import com.jnu.ticketcommon.exception.AnnounceNotExistException;
import com.jnu.ticketdomain.domains.announce.domain.Announce;
import com.jnu.ticketdomain.domains.announce.out.AnnounceLoadPort;
import com.jnu.ticketdomain.domains.announce.out.AnnounceRecordPort;
import com.jnu.ticketdomain.domains.announce.repository.AnnounceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Adaptor
@RequiredArgsConstructor
public class AnnounceAdaptor implements AnnounceRecordPort, AnnounceLoadPort {

    private final AnnounceRepository announceRepository;

    @Override
    public Announce save(Announce announce) {
        return announceRepository.save(announce);
    }

    @Override
    public Announce update(Long announceId, String title, String content) {
        Announce announce = announceRepository.findById(announceId).orElseThrow(() -> AnnounceNotExistException.EXCEPTION);
        announce.updateTitle(title);
        announce.updateContent(content);
        return announce;
    }

    @Override
    public void delete(Long announceId) {
        Announce announce =
                announceRepository
                        .findById(announceId)
                        .orElseThrow(() -> AnnounceIdNotExistException.EXCEPTION);
        announceRepository.delete(announce);
    }

    @Override
    public Page<Announce> findAllByOrderByCreatedAtDesc(Pageable pageable) {
        return announceRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public Announce findAnnounceByLastOne() {
        return announceRepository
                .findFirst1ByOrderByCreatedAtDesc()
                .orElseThrow(() -> AnnounceNotExistException.EXCEPTION);
    }

    @Override
    public Announce findById(Long announceId) {
        return announceRepository
                .findById(announceId)
                .orElseThrow(() -> AnnounceNotExistException.EXCEPTION);
    }
}
