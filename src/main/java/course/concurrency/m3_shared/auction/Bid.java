package course.concurrency.m3_shared.auction;

import java.util.Objects;

public class Bid {
    private final Long id;
    private final Long participantId;
    private final Long price;

    public Bid(Long id, Long participantId, Long price) {
        this.id = id;
        this.participantId = participantId;
        this.price = price;
    }

    public boolean moreThen(Bid other) {
        return Objects.isNull(other) || this.getPrice() > other.getPrice();
    }

    public Long getId() {
        return id;
    }

    public Long getParticipantId() {
        return participantId;
    }

    public Long getPrice() {
        return price;
    }
}
