package logic.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import logic.ScheduledExam;
import messages.CFPMessage;
import messages.Exam;
import messages.ProposalMessage;

import java.io.IOException;
import java.util.*;

public class Resource extends Agent {

    private HashSet<Exam> validExams;
    private ScheduledExam currentExam, nextExam;

    public ScheduledExam getCurrentExam() {
        return currentExam;
    }

    public ScheduledExam getNextExam() {
        return nextExam;
    }

    private static HashMap<Exam, Double> examDuration;

    static {
        examDuration = new HashMap<>();
        examDuration.put(Exam.XRAY, 1.0);
        examDuration.put(Exam.ULTRASOUND, 2.0);
        examDuration.put(Exam.MAMMOGRAM, 1.5);
        examDuration.put(Exam.CATSCAN, 2.0);
        examDuration.put(Exam.MRI, 1.0);
        examDuration.put(Exam.EKG, 1.5);
        examDuration.put(Exam.URINANALYSIS, 0.25);
        examDuration.put(Exam.COLONOSCOPY, 2.5);
        examDuration.put(Exam.PROSTATEEXAM, 0.5);
        examDuration.put(Exam.IVP, 1.25);
    }

    protected void setup() {

        validExams = new HashSet<>();
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            for (Object o : args) {
                validExams.add(Exam.valueOf((String) o));
            }
        }

        // Register the valid exams services in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        for (Exam e : validExams) {
            ServiceDescription sd = new ServiceDescription();
            sd.setType("RESOURCE");
            sd.setName(e.name());
            dfd.addServices(sd);
        }
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        ACLMessage a = blockingReceive();
        addBehaviour(new AuctionBehaviour2(this));
    }

    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // Printout a dismissal message
        System.out.println("Resource-agent " + getAID().getName() + " terminating.");
    }

    private class AuctionBehaviour2 extends SimpleBehaviour {

        private final int SENT_CFP = 0, SENT_ACCEPTANCE = 1, RECEIVED_ACCEPTANCE = 2, DONE = 3, RECEIVED_PROPOSAL = 4, RECEIVED_REJECT = 5;
        private int state;
        private HashMap<String, Integer> states;
        private HashMap<String, ACLMessage> sentCfps;
        private HashMap<String, ACLMessage> receivedProposals;
        private HashMap<String, ACLMessage> sentAcceptances;
        private HashMap<String, ACLMessage> receivedAcceptancesResponses;

        protected String getSaltString() {
            String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
            StringBuilder salt = new StringBuilder();
            Random rnd = new Random();
            while (salt.length() < 18) { // length of the random string.
                int index = (int) (rnd.nextFloat() * SALTCHARS.length());
                salt.append(SALTCHARS.charAt(index));
            }
            String saltStr = salt.toString();
            return saltStr;

        }

        private Map.Entry<String, ACLMessage> getMaxBidEntry() {
            double maxBid = -1;
            Map.Entry<String, ACLMessage> maxBidEntry = null;
            try {
                for (Map.Entry<String, ACLMessage> entry : receivedProposals.entrySet()) {
                    if (entry.getValue().getPerformative() == ACLMessage.PROPOSE && states.get(entry.getKey()) == RECEIVED_PROPOSAL) {
                        ProposalMessage p = (ProposalMessage) entry.getValue().getContentObject();
                        if (p.getBid() > maxBid) {
                            maxBid = p.getBid();
                            maxBidEntry = entry;
                        }
                    }
                }
            } catch (UnreadableException e) {
                e.printStackTrace();
            }
            return maxBidEntry;
        }

        private double getBid(Map.Entry<String, ACLMessage> e) {
            try {
                return ((ProposalMessage) e.getValue().getContentObject()).getBid();
            } catch (UnreadableException e1) {
                e1.printStackTrace();
            }
            return 0.0;
        }

        public void startAuction() {
            if (nextExam != null) {
                return;
            }
            ArrayList<ACLMessage> cfps = new ArrayList<>();
            HashMap<Exam, ArrayList<AID>> agentsNeedingExams = getAgentsNeedingExams();
            Date startingDate;
            if (((Resource) myAgent).currentExam == null)
                startingDate = new Date();
            else startingDate = ((Resource) myAgent).currentExam.getEndDate();
            for (Exam e : agentsNeedingExams.keySet()) {
                if (agentsNeedingExams.get(e).size() == 0)
                    continue;

                double duration = examDuration.get(e);
                try {
                    for (AID a : agentsNeedingExams.get(e)) {
                        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                        cfp.setConversationId(getSaltString());
                        cfp.setContentObject(new CFPMessage(startingDate, duration, e));
                        cfp.addReceiver(a);
                        cfps.add(cfp);
                       // System.out.println("Sending cfp: " + cfp);
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (cfps.size() == 0) state = DONE;
            for (ACLMessage cfp : cfps) {
                state = SENT_CFP;
                sentCfps.put(cfp.getConversationId(), cfp);
                states.put(cfp.getConversationId(), SENT_CFP);
                myAgent.send(cfp);
            }
        }


        public AuctionBehaviour2(Resource a) {
            super(a);
            states = new HashMap<>();
            sentCfps = new HashMap<>();
            receivedProposals = new HashMap<>();
            sentAcceptances = new HashMap<>();
            receivedAcceptancesResponses = new HashMap<>();

            startAuction();
        }

        private HashMap<Exam, ArrayList<AID>> getAgentsNeedingExams() {
            HashMap<Exam, ArrayList<AID>> examAgents = new HashMap<>();

            for (Exam e : ((Resource) myAgent).validExams) {
                ArrayList<AID> agents = new ArrayList<>();
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("PATIENT");
                sd.setName(e.name());
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    for (DFAgentDescription aResult : result) {
                        agents.add(aResult.getName());
                    }
                } catch (FIPAException e1) {
                    e1.printStackTrace();
                }
                examAgents.put(e, agents);
            }
            return examAgents;
        }

        private void sendRejectProposals() throws UnreadableException, IOException {
            for (String key : receivedProposals.keySet()) {
                ACLMessage msg = receivedProposals.get(key);
                //SEND TO ALL PROPOSALS THAT DIDNT REJECT ME, DONT REPEAT
                if (msg.getPerformative() == ACLMessage.PROPOSE && sentAcceptances.get(key) == null) {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    reply.setContentObject(msg.getContentObject());
                    sentAcceptances.put(msg.getConversationId(), reply);
                    myAgent.send(reply);
                }
            }
        }

        private void sendAcceptances() {
            Map.Entry<String, ACLMessage> bestProposal = getMaxBidEntry();
            if (bestProposal == null) {
                state = DONE;
                return;
            }
            try {
                ACLMessage proposal = bestProposal.getValue();
                ACLMessage reply = proposal.createReply();
                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);

                reply.setContentObject(proposal.getContentObject());
                state = SENT_ACCEPTANCE;
                states.put(proposal.getConversationId(), SENT_ACCEPTANCE);
                sentAcceptances.put(proposal.getConversationId(), reply);
                myAgent.send(reply);

                sendRejectProposals();
            } catch (IOException | UnreadableException e) {
                e.printStackTrace();
            }
        }

        private void sendFailures() {
            for (String key: receivedProposals.keySet()) {
                ACLMessage msg = receivedProposals.get(key);
                if(sentAcceptances.get(key).getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.FAILURE);
                    states.put(key, DONE);
                    myAgent.send(reply);
                }
            }
        }

        @Override
        public void action() {
            if (state == DONE) return;
            ACLMessage msg = blockingReceive();
            Resource myResource = (Resource) myAgent;
            String key = msg.getConversationId();
            ACLMessage reply = msg.createReply();
            try {
                System.out.println("Resource-Agent " + getLocalName() + " received " + ACLMessage.getPerformative(msg.getPerformative()) + " " + msg.getContentObject() + " from " + msg.getSender());
            } catch (UnreadableException e) {
                e.printStackTrace();
            }

            if (msg.getPerformative() == ACLMessage.PROPOSE || msg.getPerformative() == ACLMessage.REFUSE) {
                if (state != SENT_CFP) {
                    reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                    myAgent.send(reply);
                    return;
                }
                if (states.get(key) != SENT_CFP) {
                    reply.setPerformative(ACLMessage.UNKNOWN);
                    myAgent.send(reply);
                    return;
                }
                states.put(key, RECEIVED_PROPOSAL);
                receivedProposals.put(key, msg);
                if (receivedProposals.size() == sentCfps.size())
                    sendAcceptances();
            } else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                receivedAcceptancesResponses.put(key, msg);
                states.put(key, RECEIVED_ACCEPTANCE);
            } else if (msg.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                receivedAcceptancesResponses.put(key, msg);
                states.put(key, RECEIVED_REJECT);
                sendAcceptances();
            } else if (msg.getPerformative() == ACLMessage.INFORM) {
         //       if(states.get(key) == RECEIVED_ACCEPTANCE) {
                    sendFailures();
                    state = DONE;
                    try {
                        ScheduledExam auctionWinner = (ScheduledExam) msg.getContentObject();
                        if(auctionWinner == null) {
                            auctionWinner = (ScheduledExam) receivedAcceptancesResponses.get(msg.getConversationId()).getContentObject();
                        }
                        if (currentExam == null) currentExam = auctionWinner;
                        else nextExam = auctionWinner;
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
               // }
            }

        }

        @Override
        public boolean done() {
            if(state == DONE) {
                System.out.println(getLocalName() + " Current Exam: " + currentExam + " , Next Exam: " + nextExam);
            }
            return state == DONE;
        }
    }

    public HashSet<Exam> getValidExams() {
        return validExams;
    }
}