package logic.agents;

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

public class Patient extends Agent {

    private ArrayList<Exam> neededExams;
    private double severity;
    private double criticality;
    private ScheduledExam currentExam, nextExam;

    public Date getAvailability(Date resourceAvailiability) {
        if(currentExam == null) return resourceAvailiability;
        if(resourceAvailiability.getTime() > currentExam.getEndDate().getTime()) return resourceAvailiability;
        return currentExam.getEndDate();
    }

    protected void setup() {

        neededExams = new ArrayList<>();

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            severity = Double.parseDouble((String) args[0]);
            criticality = Double.parseDouble((String) args[1]);
            for (Object o : Arrays.copyOfRange(args, 2, args.length)) {
                neededExams.add(Exam.valueOf((String) o));
            }
        }

        System.out.println("Hello! Patient-agent " + getAID().getName() + " is ready.");

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        for(Exam e : neededExams) {
            ServiceDescription sd = new ServiceDescription();
            sd.setName(e.name());
            sd.setType("PATIENT");
            dfd.addServices(sd);
        }
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new AuctionBehaviour2(this));
    }

    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // Printout a dismissal message
        System.out.println("Patient-agent " + getAID().getName() + " terminating.");
    }

    private class AuctionBehaviour2 extends SimpleBehaviour {

        private final int RECEIVED_CFP = 0, WAITING_ACCEPTANCE = 1, RECEIVED_ACCEPTANCE = 2, RECEIVED_REJECT = 3, DONE = 4, SENT_REJECT = 5;
        private int state;
        private HashMap<String, Integer> states;
        private HashMap<String, ACLMessage> receivedCfps;
        private HashMap<String, ACLMessage> sentProposals;
        private HashMap<String, ACLMessage> receivedAcceptances;
        private HashMap<String, ACLMessage> sentAcceptancesResponses;

        private boolean isMaxProposal(double bid) {
            for(String key : sentProposals.keySet()) {
                if(sentProposals.get(key).getPerformative() == ACLMessage.PROPOSE) {
                    try {
                        ProposalMessage p = (ProposalMessage) sentProposals.get(key).getContentObject();
                        if(p.getBid() > bid) return false;
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
                }
            }
            return true;
        }

        private Map.Entry<String, ACLMessage> getMaxBidEntry() {
            double maxBid = 0;
            Map.Entry<String, ACLMessage> maxBidEntry = null;
            try {
                for (Map.Entry<String, ACLMessage> entry : receivedAcceptances.entrySet()) {
                    if(entry.getValue().getPerformative() == ACLMessage.ACCEPT_PROPOSAL && states.get(entry.getKey()) == RECEIVED_ACCEPTANCE) {
                        ProposalMessage p = (ProposalMessage) entry.getValue().getContentObject();
                        if(p.getBid() > maxBid) {
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


        public AuctionBehaviour2(Patient a) {
            super(a);
            states = new HashMap<>();
            receivedCfps = new HashMap<>();
            sentProposals = new HashMap<>();
            receivedAcceptances = new HashMap<>();
            sentAcceptancesResponses = new HashMap<>();
            state = 0;
        }

        @Override
        public void action() {
           ACLMessage msg = blockingReceive();
           Patient myPatient = (Patient) myAgent;
           String key = msg.getConversationId();
           ACLMessage reply = msg.createReply();

            try {
                System.out.println("Patient-Agent " + getLocalName() + " received " + ACLMessage.getPerformative(msg.getPerformative()) + " " + msg.getContentObject() + " from " + msg.getSender());
            } catch (UnreadableException e) {
                e.printStackTrace();
            }

           if(msg.getPerformative() == ACLMessage.CFP) {
               try {
                   //states.put(key, 1);
                   CFPMessage content = (CFPMessage) msg.getContentObject();
                   //System.out.println("Patient-Agent " + getLocalName() + " received: " + content);
                   if(myPatient.currentExam != null && myPatient.nextExam != null) {
                       this.myAgent.send(new ACLMessage(ACLMessage.REFUSE));
                       return;
                   }
                   states.put(key, RECEIVED_CFP);
                   receivedCfps.put(key, msg);
                   reply.setPerformative(ACLMessage.PROPOSE);
                   Date startingDate = getAvailability(content.getAvailableTime());
                   long duration = (long) (content.getDuration()*60*60*1000);
                   double bid = severity * duration + criticality/2.0*duration*duration;
                   ProposalMessage proposal = new ProposalMessage(startingDate, bid);
                   try {
                       reply.setContentObject(proposal);
                      // System.out.println("Sending proposal: " + reply + " -> " + getLocalName());
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
                   sentProposals.put(key, reply);
                   states.put(key, WAITING_ACCEPTANCE);
                   this.myAgent.send(reply);
               } catch (UnreadableException e) {
                   e.printStackTrace();
               }
           }
           else if(msg.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
               states.put(key, RECEIVED_REJECT);
               receivedAcceptances.put(key, msg);
               if(receivedCfps.size() == receivedAcceptances.size() && getMaxBidEntry() != null) {
                   sendInform(msg);
               }
           }
           else if(msg.getPerformative() == ACLMessage.FAILURE) {
                states.put(key, RECEIVED_REJECT);
                receivedAcceptances.put(key, msg);
                if(receivedCfps.size() == receivedAcceptances.size() && getMaxBidEntry() != null) {
                   sendInform(msg);
               }
               if(receivedCfps.size() == receivedAcceptances.size()) {
                    state = DONE;
               }
           }
           else if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
              if(states.get(key) != WAITING_ACCEPTANCE && states.get(key) != RECEIVED_REJECT) {
                   myAgent.send(new ACLMessage(ACLMessage.NOT_UNDERSTOOD));
                   return;
               }
               try {
                   ProposalMessage prop = (ProposalMessage) msg.getContentObject();
                   Map.Entry<String, ACLMessage> maxBidEntry = getMaxBidEntry();
                   if(maxBidEntry == null) {
                       //TODO: BUG HERE
                       reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                       Exam e = ((CFPMessage) receivedCfps.get(msg.getConversationId()).getContentObject()).getExam();
                       double duration = ((CFPMessage) receivedCfps.get(msg.getConversationId()).getContentObject()).getDuration();
                       Date startingDate = ((ProposalMessage) sentProposals.get(msg.getConversationId()).getContentObject()).getStartingDate();

                       ScheduledExam scheduledExam = new ScheduledExam(startingDate, duration, e, myAgent.getAID());
                       if(new Date().getTime() >= startingDate.getTime()) currentExam = scheduledExam;
                       else nextExam = scheduledExam;
                       reply.setContentObject(scheduledExam);
                       states.put(key, RECEIVED_ACCEPTANCE);
                       receivedAcceptances.put(key, msg);
                       sentAcceptancesResponses.put(key, reply);
                       if(receivedCfps.size() == receivedAcceptances.size()) {
                           state = DONE;
                           reply.setPerformative(ACLMessage.INFORM);
                       }
                       myAgent.send(reply);
                   } else {
                       double maxBid = getBid(maxBidEntry);
                       receivedAcceptances.put(key, msg);
                       states.put(key, RECEIVED_ACCEPTANCE);
                       if (prop.getBid() <= maxBid) {
                           reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                           states.put(key, SENT_REJECT);
                           sentAcceptancesResponses.put(key, reply);
                           myAgent.send(reply);
                       } else {
                           ACLMessage rejectOld = maxBidEntry.getValue().createReply();
                           rejectOld.setPerformative(ACLMessage.REJECT_PROPOSAL);
                           myAgent.send(rejectOld);
                           sentAcceptancesResponses.put(rejectOld.getConversationId(), rejectOld);

                           reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                           Exam e = ((CFPMessage) receivedCfps.get(msg.getConversationId()).getContentObject()).getExam();
                           double duration = ((CFPMessage) receivedCfps.get(msg.getConversationId()).getContentObject()).getDuration();
                           Date startingDate = ((ProposalMessage) sentProposals.get(msg.getConversationId()).getContentObject()).getStartingDate();

                           ScheduledExam scheduledExam = new ScheduledExam(startingDate, duration, e, myAgent.getAID());
                           if (new Date().getTime() >= startingDate.getTime()) currentExam = scheduledExam;
                           else nextExam = scheduledExam;
                           reply.setContentObject(scheduledExam);
                           if (isMaxProposal(prop.getBid())) {
                               reply.setPerformative(ACLMessage.INFORM);
                               state = DONE;
                           }
                           myAgent.send(reply);
                       }
                   }
                   if(receivedCfps.size() == receivedAcceptances.size() && getMaxBidEntry() != null) {
                       state = DONE;
                       Exam e = ((CFPMessage) receivedCfps.get(msg.getConversationId()).getContentObject()).getExam();
                       double duration = ((CFPMessage) receivedCfps.get(msg.getConversationId()).getContentObject()).getDuration();
                       Date startingDate = ((ProposalMessage) sentProposals.get(msg.getConversationId()).getContentObject()).getStartingDate();

                       ScheduledExam scheduledExam = new ScheduledExam(startingDate, duration, e, myAgent.getAID());
                       reply.setContentObject(scheduledExam);
                       reply = getMaxBidEntry().getValue().createReply();
                       reply.setPerformative(ACLMessage.INFORM);
                       myAgent.send(reply);
                   }
               } catch (UnreadableException | IOException e) {
                   e.printStackTrace();
               }
           }
        }

        private void sendInform(ACLMessage msg) {
            ACLMessage reply;
            state = DONE;
            try {
                Exam e = ((CFPMessage) receivedCfps.get(msg.getConversationId()).getContentObject()).getExam();
                double duration = ((CFPMessage) receivedCfps.get(msg.getConversationId()).getContentObject()).getDuration();
                Date startingDate = ((ProposalMessage) sentProposals.get(msg.getConversationId()).getContentObject()).getStartingDate();

                ScheduledExam scheduledExam = new ScheduledExam(startingDate, duration, e, myAgent.getAID());
                reply = getMaxBidEntry().getValue().createReply();
                reply.setPerformative(ACLMessage.INFORM);
                myAgent.send(reply);
            } catch (UnreadableException e1) {
                e1.printStackTrace();
            }
        }

        @Override
        public boolean done() {
            if(state == DONE) {
                System.out.println(getLocalName() + " Current Exam: " + currentExam + " , Next Exam: " + nextExam);
            }
            return state==DONE;
        }
    }

}
