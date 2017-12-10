package messages;

import java.io.Serializable;
import java.util.Date;

public class ProposalMessage implements Serializable, Comparable {

    Date startingDate;
    double bid;


    public ProposalMessage(Date startingDate, double bid) {
        this.startingDate = startingDate;
        this.bid = bid;
    }

    public Date getStartingDate() {
        return startingDate;
    }

    @Override
    public String toString() {
        return "ProposalMessage{" +
                "startingDate=" + startingDate +
                ", bid=" + bid +
                '}';
    }

    public double getBid() {
        return bid;
    }


    @Override
    public int compareTo(Object o) {
        if (!(o instanceof ProposalMessage))
            throw new ClassCastException();
        if (this == o) return 0;
        return Double.compare(bid, ((ProposalMessage) o).bid);
    }
}
