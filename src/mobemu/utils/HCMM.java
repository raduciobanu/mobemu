/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.utils;

import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JTextArea;

/**
 *
 * This mobility model is based on the one described in the scientific paper
 * entitled "Users Mobility Models for Opportunistic Networks: the Role of
 * Physical Locations" (Chiara Boldrini, Marco Conti, Andrea Passarella), which
 * is in turn based on the model described in "Designing Mobility Models based
 * on Social Network Theory" (Mirco Musolesi and Cecilia Mascolo). The license
 * file (HCMM_license.txt) can be found in the "licenses" folder.
 *
 * @author Radu
 */
public abstract class HCMM {

    /**
     * set this to true if you want a single inter-community node
     */
    protected boolean controlledRewiring = false;
    /*
     * number of hosts
     */
    protected int numHosts = 1000;
    /**
     * connection threshold
     */
    protected double connectionThreshold = 0.1;
    /**
     * length of the sides of the area
     */
    protected double sideLengthWholeAreaX = 2000.0;
    protected double sideLengthWholeAreaY = 2000.0;
    /**
     * radius of the transmission area
     */
    protected double radius = 200.0;
    /**
     * number of rows
     */
    protected int numberOfRows = 20;
    /**
     * number of columns
     */
    protected int numberOfColumns = 20;
    /**
     * total simulation time
     */
    protected double totalSimulationTime = 1000.0;
    /**
     * refresh time
     */
    protected double stepInterval = 1.0;
    /**
     * low bound speed of the host
     */
    protected double minHostSpeed = 1.0;
    protected double maxHostSpeed = 10.00;
    /**
     * reconfiguration interval (interval between two reconfigurations)
     */
    protected double reconfigurationInterval = 10000.0;
    /**
     * rewiring probability
     */
    protected double rewiringProb = 0.1;
    /**
     * number of communities
     */
    protected int numberOfGroups = 10;
    /**
     * simulation time
     */
    protected double simTime = 0.0;
    /**
     * number of traveler nodes
     */
    protected int numberOfTravelers = 0;
    /**
     * speed of the traveler nodes
     */
    protected double travelerSpeed = 20.0;
    /**
     * HIBOP: probability of remaining in the same group for a while
     */
    protected double travProb = 0;
    /*
     * verbosity level
     */
    protected boolean printOut = false;
    /**
     * Girvan-Newman clustering algorithm
     */
    protected boolean girvanNewman = false;
    /**
     * communities traces
     */
    protected boolean communitiesTraces = true;
    /**
     * deterministic on/off
     */
    protected boolean deterministic = true;
    /**
     * NS trace
     */
    protected boolean nsTrace = false;
    /**
     * drift
     */
    protected float drift;
    /**
     * first run of the algorithm
     */
    protected boolean firstTime = true;
    /*
     * probability of remaining in a non-home cell
     */
    protected double remainingProb = 0;
    /**
     * set to true if the HCMM simulation should be shown graphically
     */
    protected boolean showRun = false;
    /**
     * sleep time between simulation steps
     */
    protected long sleepTime = 5;
    /*
     * internal data structures
     */
    private Host[] hosts;
    protected int[][] groups;
    protected int[] numberOfMembers;
    protected double[] distances;
    FileWriter fstream;
    BufferedWriter out = null;
    /**
     * random number generator seed
     */
    private int seed = 0;

    /**
     * Constructor for a {@code HCMM} object.
     *
     * @param seed random number generator seed
     */
    public HCMM(int seed) {
        hosts = new Host[numHosts];
        distances = new double[numHosts];

        for (int i = 0; i < numHosts; i++) {
            hosts[i] = new Host();
            distances[i] = 0;
        }

        numberOfMembers = new int[numHosts];
        this.seed = seed;
    }

    /**
     * Constructor for a {@code HCMM} object with seed 0.
     */
    public HCMM() {
        this(0);
    }

    public void setNumHosts(int numHosts) {
        this.numHosts = numHosts;

        hosts = new Host[numHosts];

        for (int i = 0; i < numHosts; i++) {
            hosts[i] = new Host();
        }

        numberOfMembers = new int[numHosts];
    }

    public void setMinHostSpeed(double minHostSpeed) {
        this.minHostSpeed = minHostSpeed;
    }

    public void setMaxHostSpeed(double maxHostSpeed) {
        this.maxHostSpeed = maxHostSpeed;
    }

    public void setConnectionThreshold(double connectionThreshold) {
        this.connectionThreshold = connectionThreshold;
    }

    public void setSideLengthWholeAreaX(double sideLengthWholeAreaX) {
        this.sideLengthWholeAreaX = sideLengthWholeAreaX;
    }

    public void setSideLengthWholeAreaY(double sideLengthWholeAreaY) {
        this.sideLengthWholeAreaY = sideLengthWholeAreaY;
    }

    public void setNumberOfRows(int numberOfRows) {
        this.numberOfRows = numberOfRows;
    }

    public void setNumberOfColumns(int numberOfColumns) {
        this.numberOfColumns = numberOfColumns;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public void setTotalSimulationTime(double totalSimulationTime) {
        this.totalSimulationTime = totalSimulationTime;
    }

    public void setReconfigurationInterval(double reconfigurationInterval) {
        this.reconfigurationInterval = reconfigurationInterval;
    }

    public void setRewiringProb(double rewiringProb) {
        this.rewiringProb = rewiringProb;
    }

    public void setNumberOfGroups(int numberOfGroups) {
        this.numberOfGroups = numberOfGroups;
    }

    public void setNumberOfTravelers(int numberOfTravelers) {
        this.numberOfTravelers = numberOfTravelers;
    }

    public void setTravelerSpeed(double travelerSpeed) {
        this.travelerSpeed = travelerSpeed;
    }

    public void setGirvanNewman(boolean girvanNewman) {
        this.girvanNewman = girvanNewman;
    }

    public void setCommunitiesTraces(boolean communitiesTraces) {
        this.communitiesTraces = communitiesTraces;
    }

    public void setDeterministic(boolean deterministic) {
        this.deterministic = deterministic;
    }

    public void setRemainingProb(double remainingProb) {
        this.remainingProb = remainingProb;
    }

    public void setNsTrace(boolean nsTrace) {
        this.nsTrace = nsTrace;
    }

    public void setShowRun(boolean showRun) {
		this.showRun = showRun;
    }

    public void setSleepTime(long sleepTime) {
		this.sleepTime = sleepTime;
    }

    public int getNumHosts() {
        return numHosts;
    }

    public Host[] getHosts() {
		return hosts;
    }

    public int getNumberOfRows() {
        return numberOfRows;
    }

    public int getNumberOfColumns() {
        return numberOfColumns;
    }

    public int getNumberOfGroups() {
        return numberOfGroups;
    }

    public double getRadius() {
        return radius;
    }

    public int[][] getGroups() {
        return groups;
    }

    public int[] getNumberOfMembers() {
        return numberOfMembers;
    }

    public double getGridWidth() {
        return sideLengthWholeAreaX;
    }

    public double getGridHeight() {
        return sideLengthWholeAreaY;
    }

    /**
     * Generates the adjacency matrix from the weight matrix.
     *
     * @param weightMat weight matrix
     * @param adjacencyMat adjacency matrix
     * @param threshold adjacency threshold
     * @param arraySize size of the arrays
     */
    private void generateAdjacency(double[][] weightMat, int[][] adjacencyMat,
            double threshold, int arraySize) {
        for (int i = 0; i < arraySize; i++) {
            for (int j = 0; j < arraySize; j++) {
                if (weightMat[i][j] > threshold) {
                    adjacencyMat[i][j] = 1;
                } else {
                    adjacencyMat[i][j] = 0;
                }
            }
        }
    }

    /**
     * Checks if a node belongs to a group.
     *
     * @param node node to be checked
     * @param group group to be searched
     * @param numberOfMembers number of members in the group
     * @return {@code true} if the node is in the group, {@code false} otherwise
     */
    private boolean isInGroup(int node, int[] group, int numberOfMembers) {
        for (int k = 0; k < numberOfMembers; k++) {
            if (group[k] == node) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if two nodes are in a common group.
     *
     * @param node1 first node
     * @param node2 second node
     * @param groups groups to check
     * @param numberOfGroups number of groups
     * @param numberOfMembers number of members per group
     * @return {@code true} if the two nodes belong to a common group, {@code false}
     * otherwise
     */
    protected boolean areInTheSameGroup(int node1, int node2, int[][] groups,
            int numberOfGroups, int[] numberOfMembers) {
        for (int k = 0; k < numberOfGroups; k++) {
            if (isInGroup(node1, groups[k], numberOfMembers[k])) {
                if (isInGroup(node2, groups[k], numberOfMembers[k])) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Initializes the weight matrix composed of n disjointed groups with n
     * rewiring.
     *
     * @param weight weight matrix resulted
     * @param arraySize size of the weight matrix
     * @param numberOfGroups number of groups
     * @param probRewiring the rewiring probability
     * @param threshold threshold under which the current relationship is not
     * considered
     */
    private void initializeWeightArrayInGroups(double[][] weight, int arraySize,
            int numberOfGroups, double probRewiring, double threshold) {
        int[][] currentGroups = new int[arraySize][arraySize];
        int[] currentNumberOfMembers = new int[numberOfGroups];

        for (int i = 0; i < numberOfGroups; i++) {
            currentNumberOfMembers[i] = 0;
        }

        for (int i = 0; i < arraySize; i++) {
            int groupId = i % numberOfGroups;
            currentGroups[groupId][currentNumberOfMembers[groupId]] = i + 1;
            currentNumberOfMembers[groupId] += 1;
        }

        int countRew = 0;
        int countLinks = 0;

        /*
         * Weight matrix after rewiring is not symmetric.
         */
        for (int i = 0; i < arraySize; i++) {

            Random rand = new Random(seed);

            for (int j = 0; j < arraySize; j++) {
                if (i != j) {
                    if (areInTheSameGroup(i + 1, j + 1, currentGroups, numberOfGroups, currentNumberOfMembers)) {
                        countLinks++;
                        if ((rand.nextDouble()) < probRewiring) {
                            // rewiring
                            countRew++;
                            boolean found = false;
                            for (int z = 0; z < arraySize; z++) {
                                if (!areInTheSameGroup(i + 1, z + 1, currentGroups, numberOfGroups, currentNumberOfMembers)
                                        && (weight[i][z] < threshold) && !found) {
                                    weight[i][z] = 1.0 - (rand.nextDouble() * threshold);
                                    found = true;
                                }
                            }

                            weight[i][j] = (rand.nextDouble() * threshold);
                        } else {
                            // no rewiring
                            weight[i][j] = 1.0 - (rand.nextDouble() * threshold);
                        }
                    } else {
                        // the hosts are not in the same cluster
                        weight[i][j] = (rand.nextDouble() * threshold);
                    }
                }
            }
        }
    }

    /**
     * Assigns distances based on an adjacency matrix.
     *
     * @param distance distance array to be updated
     * @param d distance to be updated
     * @param assigned array of assigned values
     * @param adjacency adjacency matrix
     * @param pred prediction matrix
     * @param predNum number of elements for each prediction vector
     * @param arraySize size of the distance array
     */
    private void assignDistance(double[] distance, int d, int[] assigned, int[][] adjacency,
            int[][] pred, int[] predNum, int arraySize) {
        for (int k = 0; k < arraySize; k++) {
            if (distance[k] == d) {
                for (int r = 0; r < arraySize; r++) {
                    if (adjacency[k][r] == 1) {
                        if (assigned[r] == 1) {
                            distance[r] = d + 1;
                            assigned[r] = 0;
                            boolean found = false;
                            for (int p = 0; p < predNum[r]; p++) {
                                if (pred[r][p] == k) {
                                    found = true;
                                }
                            }

                            if (!found) {
                                pred[r][predNum[r]] = k;
                                predNum[r] = predNum[r] + 1;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Computes betweenness for a single vertex.
     *
     * @param betweenness betweenness array to be filled
     * @param distance distance array
     * @param pred prediction matrix
     * @param predNum number of elements for each prediction vector
     * @param arraySize size of the betweenness array
     */
    private void computeSingleBetweenness(double[] betweenness, double[] distance,
            int[][] pred, int[] predNum, int arraySize) {
        for (int dist = 15; dist > -1; dist--) {
            for (int k = 0; k < arraySize; k++) {
                if (distance[k] == dist) {
                    for (int i = 0; i < predNum[k]; i++) {
                        betweenness[pred[k][i]] = betweenness[pred[k][i]] + betweenness[k] / predNum[k];
                    }

                }
            }
        }
    }

    /**
     * Computes betweenness.
     *
     * @param resultBetweenness the resulting betweenness array
     * @param adjacency the adjacency matrix
     * @param arraySize the size of the betweenness array
     */
    private void computeBetweenness(double[] resultBetweenness, int[][] adjacency, int arraySize) {
        double[] distance = new double[arraySize];
        int[] assigned = new int[arraySize];
        int[] predNum = new int[arraySize];
        double[] betw = new double[arraySize];
        double[] currentBetweenness = new double[arraySize];

        for (int i = 0; i < arraySize; i++) {
            assigned[i] = 1;
            distance[i] = 15;
            betw[i] = 1;
            currentBetweenness[i] = 0;
        }

        int[][] pred = new int[arraySize][arraySize];

        for (int i = 0; i < arraySize; i++) {
            for (int s = 0; s < arraySize; s++) {
                assigned[s] = 1;
                distance[s] = 16;
                predNum[s] = 0;
                betw[s] = 1;
            }

            assigned[i] = 0;
            distance[i] = 0;
            for (int d = 0; d < 15; d++) {
                assignDistance(distance, d, assigned, adjacency, pred, predNum, arraySize);
            }

            computeSingleBetweenness(betw, distance, pred, predNum, arraySize);

            for (int z = 0; z < arraySize; z++) {
                currentBetweenness[z] = currentBetweenness[z] + betw[z];
            }
        }

        System.arraycopy(currentBetweenness, 0, resultBetweenness, 0, arraySize);
    }

    /**
     * Assigns a node to a group.
     *
     * @param current node to be added to a group
     * @param currentGroup group the node is added to
     * @param adjacency adjacency matrix
     * @param groups groups matrix
     * @param numberOfMembers number of members per group
     * @param arraySize size of the arrays
     * @param assigned vector of assigned values
     */
    private void assignToAGroup(int current, int currentGroup, int[][] adjacency, int[][] groups,
            int[] numberOfMembers, int arraySize, boolean[] assigned) {
        for (int k = 0; k < arraySize; k++) {
            if (adjacency[current][k] == 1) {
                if (current != k) {
                    if (assigned[k] == false) {
                        numberOfMembers[currentGroup - 1] = numberOfMembers[currentGroup - 1] + 1;
                        groups[currentGroup - 1][numberOfMembers[currentGroup - 1] - 1] = k + 1;
                        assigned[k] = true;
                        assignToAGroup(k, currentGroup, adjacency, groups, numberOfMembers, arraySize, assigned);
                    }
                }
            }
        }
    }

    /**
     * Returns the number of groups given an adjacency matrix.
     *
     * @param adjacency adjacency matrix
     * @param groups groups matrix
     * @param numberOfMembers number of members per group
     * @param arraySize size of the adjacency matrix
     * @return the number of groups
     */
    private int getGroups(int[][] adjacency, int[][] groups, int[] numberOfMembers, int arraySize) {

        int groupsNumber = 0;
        boolean[] assigned = new boolean[arraySize];

        for (int i = 0; i < arraySize; i++) {
            assigned[i] = false;
        }

        for (int i = 0; i < arraySize; i++) {
            if (!assigned[i]) {
                groupsNumber++;
                numberOfMembers[groupsNumber - 1] = numberOfMembers[groupsNumber - 1] + 1;
                groups[groupsNumber - 1][numberOfMembers[groupsNumber - 1] - 1] = i + 1;
                assigned[i] = true;
                assignToAGroup(i, groupsNumber, adjacency, groups, numberOfMembers, arraySize, assigned);
            }
        }

        return groupsNumber;
    }

    /**
     * Prints the first n groups.
     *
     * @param numberOfGroups number of groups to be printed (n)
     * @param groups groups matrix
     * @param numberOfMembers number of members per group
     * @param arraySize size of the arrays
     */
    private void printGroups(int numberOfGroups, int[][] groups, int[] numberOfMembers, int arraySize) {
        try {
            out.write("Communities");
            out.newLine();
            out.write("===========");
            out.newLine();
        } catch (IOException ex) {
            Logger.getLogger(HCMM.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (int i = 0; i < numberOfGroups; i++) {
            try {
                out.write(String.format("Community %01d: ", (i + 1)));

                for (int j = 0; j < numberOfMembers[i]; j++) {
                    out.write(String.format("%02d ", groups[i][j]));
                }

                out.newLine();
            } catch (IOException ex) {
                Logger.getLogger(HCMM.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        try {
            out.newLine();
            out.write("Cell attractivity");
            out.newLine();
            out.write("=================");
            out.newLine();
        } catch (IOException ex) {
            Logger.getLogger(HCMM.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Splits the social network into communities, removing some links based on
     * each node's betweenness.
     *
     * @param adjacency adjacency matrix
     * @param arraySize size of the adjacency matrix
     */
    private void splitNetwork(int[][] adjacency, int arraySize) {
        int best = 0;

        boolean oneFound = false;
        for (int i = 0; i < arraySize; i++) {
            if ((adjacency[best][i] == 1) && (best != i) && (oneFound == false)) {
                adjacency[best][i] = 0;
                adjacency[i][best] = 0;
                oneFound = true;
            }
        }
    }

    /**
     * Splits the network only if the modularity of the division is higher than
     * the given threshold.
     *
     * @param adjacency adjacency matrix
     * @param arraySize size of the arrays
     * @param modThreshold modularity threshold
     * @return the new modularity if a split has been made, -1 otherwise
     */
    private double splitNetworkByThreshold(int[][] adjacency, int arraySize, double modThreshold) {
        double numberOfLinksBefore = 0;

        int[][] groupsBefore = new int[arraySize][arraySize];
        int[][] groupsAfter = new int[arraySize][arraySize];

        double[][] totalResults = new double[arraySize][arraySize];
        int[][] adjacencyBefore = new int[arraySize][arraySize];

        // adjacencyBefore is the connectivity matrix before the splitting procedure
        for (int i = 0; i < arraySize; i++) {
            System.arraycopy(adjacency[i], 0, adjacencyBefore[i], 0, arraySize);
        }

        int[] numberOfMembersBefore = new int[arraySize];
        int[] numberOfMembersAfter = new int[arraySize];

        for (int i = 0; i < arraySize; i++) {
            numberOfMembersBefore[i] = 0;
            numberOfMembersAfter[i] = 0;
        }

        getGroups(adjacency, groupsBefore, numberOfMembersBefore, arraySize);

        splitNetwork(adjacency, arraySize);

        // the adjacency is not renamed but it must be considered adjacencyAfter
        int numberOfGroupsAfter = getGroups(adjacency, groupsAfter, numberOfMembersAfter, arraySize);

        double[][] modularity = new double[arraySize][arraySize];
        double[][] modularityNum = new double[arraySize][arraySize];

        for (int i = 0; i < numberOfGroupsAfter; i++) {
            for (int j = 0; j < numberOfGroupsAfter; j++) {
                modularityNum[i][j] = 0;
            }
        }

        for (int i = 0; i < arraySize; i++) {
            for (int j = 0; j < arraySize; j++) {
                if (adjacencyBefore[i][j] == 1) {
                    numberOfLinksBefore = numberOfLinksBefore + 1;
                }
            }
        }

        for (int i = 0; i < arraySize; i++) {
            for (int j = 0; j < arraySize; j++) {
                if (adjacencyBefore[i][j] == 1) {
                    for (int g1 = 0; g1 < numberOfGroupsAfter; g1++) {
                        if (isInGroup(i + 1, groupsAfter[g1], numberOfMembersAfter[g1])) {
                            for (int g2 = 0; g2 < numberOfGroupsAfter; g2++) {
                                if (isInGroup(j + 1, groupsAfter[g2], numberOfMembersAfter[g2])) {
                                    modularityNum[g1][g2] = modularityNum[g1][g2] + 1;
                                }
                            }
                        }
                    }
                }
            }
        }

        for (int i = 0; i < numberOfGroupsAfter; i++) {
            for (int j = 0; j < numberOfGroupsAfter; j++) {
                modularity[i][j] = modularityNum[i][j] / numberOfLinksBefore;
            }
        }

        // computation of the modularity Q:
        // Q = Tr(e) - ||e^2||
        // where ||x|| indicates the sum of all elements of x

        // computation of the trace of the matrix e
        double total1 = 0;
        for (int i = 0; i < numberOfGroupsAfter; i++) {
            total1 = total1 + modularity[i][i];
        }

        for (int i = 0; i < numberOfGroupsAfter; i++) {
            for (int j = 0; j < numberOfGroupsAfter; j++) {
                double partial = 0;
                for (int t = 0; t < numberOfGroupsAfter; t++) {
                    totalResults[i][j] = partial + modularity[i][t] * modularity[t][j];
                }
            }
        }

        // computation of ||e^2||
        double total2 = 0;
        for (int i = 0; i < numberOfGroupsAfter; i++) {
            for (int j = 0; j < numberOfGroupsAfter; j++) {
                total2 = total2 + totalResults[i][j];
            }
        }

        // difference between the two parts Tr(e) and ||e^2||
        double q = total1 - total2;

        if (numberOfGroupsAfter == arraySize) {
            return -1;
        }

        if (q > modThreshold) {
            return q;
        } else {
            for (int i = 0; i < arraySize; i++) {
                System.arraycopy(adjacencyBefore[i], 0, adjacency[i], 0, arraySize);
            }

            return -1;
        }
    }

    /**
     * Initializes the weight matrix by creating a matrix composed of n
     * disjointed groups with n rewiring (this function is used when the
     * rewiring is controlled)
     *
     * @param weight weight matrix resulted
     * @param arraySize size of the weight matrix
     * @param numberOfGroups number of groups
     * @param threshold the value under which the relationship is not considered
     * important
     */
    private void initializeWeightArrayInGroups(double[][] weight,
            int arraySize, int numberOfGroups, double threshold) {
        Random rand = new Random(seed);

        int[][] currentGroups = new int[arraySize][arraySize];
        int[] currentNumberOfMembers = new int[numberOfGroups];

        for (int i = 0; i < numberOfGroups; i++) {
            currentNumberOfMembers[i] = 0;
        }

        for (int i = 0; i < arraySize; i++) {
            int groupId = i % numberOfGroups;
            currentGroups[groupId][currentNumberOfMembers[groupId]] = i + 1;
            currentNumberOfMembers[groupId] += 1;
        }

        if (arraySize / numberOfGroups < numberOfGroups) {
            return;
        }

        int traveler = 0;
        for (int gr = 1; gr < numberOfGroups; gr++) {
            // the traveler and gr are not from the same group
            boolean found = false;
            for (int z = 0; z < arraySize; z++) {
                // find a node from the same group as the traveler...
                if (areInTheSameGroup(traveler + 1, z + 1, currentGroups, numberOfGroups, currentNumberOfMembers)
                        && weight[traveler][z] > threshold && traveler != z && !found) {

                    // ... and reset their association
                    weight[traveler][z] = (rand.nextDouble() * threshold);
                    found = true;
                }
            }

            // associate the traveler and gr
            weight[traveler][gr] = 1 - (rand.nextDouble() * threshold);
            traveler = traveler + numberOfGroups;
        }
    }

    /**
     * Checks whether the given position is the home cell.
     *
     * @param nowX current X position
     * @param nowY current Y position
     * @param homeX home X position
     * @param homeY home Y position
     * @return {@code true} if the current position is the home cell, {@code false}
     * otherwise
     */
    private boolean isHome(int nowX, int nowY, int homeX, int homeY) {
        return (nowX == homeX && nowY == homeY);
    }

	private String generateStatsString(double simTime, long contacts) {
		String s = "";

		s += "Simulation time: " + (long) simTime + " seconds";
		s += System.getProperty("line.separator");
		s += "Nodes: " + numHosts + " (" + numberOfGroups + " communities, " + numberOfTravelers + " travelers)";
		s += System.getProperty("line.separator");
		s += "Grid size: " + numberOfRows + "x" + numberOfColumns + " cells, " + (int) getGridWidth() + "x"
				+ (int) getGridHeight() + " metres";
		s += System.getProperty("line.separator");
		s += "Contacts: " + contacts;

		return s;
	}

    /**
     * Runs the HCCM simulation.
     */
    protected void move() {
		long contacts = 0;
		JFrame frame = null;
		JTextArea text = null;
		HCMMComponent component = null;

		if (showRun) {
			frame = new JFrame();
			text = new JTextArea();
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.setVisible(false);
			text.setEditable(false);
			component = new HCMMComponent(this);
			frame.getContentPane().add(component, BorderLayout.CENTER);
			frame.getContentPane().add(text, BorderLayout.SOUTH);
			frame.pack();
			frame.setTitle("Simulation");

			// Display the frame
			int frameWidth = 700;
			int frameHeight = 700;
			frame.setSize(frameWidth, frameHeight);
			frame.setVisible(true);
		}

        boolean[][] isConnected = new boolean[numHosts][numHosts];
        boolean[] isConnectedWithAP = new boolean[numHosts];

        Host accessPoint = new Host(); // AP element

        CellsItem[][] cells = new CellsItem[numberOfRows][numberOfColumns];

        for (int i = 0; i < numberOfRows; i++) {
            for (int j = 0; j < numberOfColumns; j++) {
                cells[i][j] = new CellsItem();
            }
        }

        double sideLengthX;
        double sideLengthY;

        // cell attractivity
        float[][] CA = new float[numberOfRows][numberOfColumns];

        // variables used for the generation of the mobility scenario using the results
        // of complex networks theory and in particular the Girvan-Newman algorithm

        // interaction->adjacency threshold
        double threshold = connectionThreshold;

        int[][] adjacency = new int[numHosts][numHosts];
        double[][] interaction = new double[numHosts][numHosts];

        // last values registered
        double[][] lastValues = new double[numHosts][numHosts];
        double[] lastValuesWithAP = new double[numHosts];

        int[][] communities = new int[numHosts][2];

        // probability of moving to the cell [c][r]
        float[][] a = new float[numberOfRows][numberOfColumns];

        ProbRange[][] p = new ProbRange[numberOfRows][numberOfColumns];

        for (int i = 0; i < numberOfRows; i++) {
            for (int j = 0; j < numberOfColumns; j++) {
                p[i][j] = new ProbRange();
            }
        }

        try {
            fstream = new FileWriter("statistics.txt");
            out = new BufferedWriter(fstream);
        } catch (Exception e) {
            Logger.getLogger(HCMM.class.getName()).log(Level.SEVERE, null, e);
        }

        // community variables
        int newComm; // the community the node is currently in
        int[] currComm = new int[numHosts]; // the community the node was in during last movement
        double[] currCommStartTime = new double[numHosts]; // time the node has entered the community
        double[][] frequencies = new double[numHosts][numberOfRows * numberOfColumns];
        int[] previousGoalCommunity = new int[numHosts];
        boolean[][] friendCommunity = new boolean[numHosts][numberOfRows * numberOfColumns];

        for (int i = 0; i < numHosts; i++) {
            for (int j = 0; j < numberOfRows * numberOfColumns; j++) {
                frequencies[i][j] = 0;
            }
        }

        // model initialization
        sideLengthX = sideLengthWholeAreaX / ((double) numberOfRows);
        sideLengthY = sideLengthWholeAreaY / ((double) numberOfColumns);

        for (int i = 0; i < numberOfRows; i++) {
            for (int j = 0; j < numberOfColumns; j++) {
                cells[i][j].minX = ((double) i) * sideLengthX;
                cells[i][j].minY = ((double) j) * sideLengthY;
                cells[i][j].numberOfHosts = 0;
            }
        }

        // setup of the links
        for (int i = 0; i < numHosts; i++) {
            for (int l = 0; l < numHosts; l++) {
                isConnected[i][l] = false;
                lastValues[i][l] = 0.0;
            }

            isConnectedWithAP[i] = false;
            lastValuesWithAP[i] = 0.0;
        }

        // travelers initialization
        for (int i = 0; i < numberOfTravelers; i++) {
            hosts[i].isATraveler = true;

            // definition of the initial speeds of the travelers
            hosts[i].speed = travelerSpeed;
        }

        for (int i = numberOfTravelers; i < numHosts; i++) {
            hosts[i].isATraveler = false;

            Random rand = new Random(seed);

            // use the formula proposed by Camp et al. for the stazionary version of RWP
            // use tationary distribution for the random waypoint mobility model
            double unif = rand.nextDouble();
            hosts[i].speed = Math.pow(maxHostSpeed, unif) / Math.pow(minHostSpeed, unif - 1);
            // in the original CMM: hosts[i].speed = generateRandomDouble(minHostSpeed, maxHostSpeed);
        }

        accessPoint.speed = 0;

        double numberOfReconfigurations = 0.0;
        double nextReconfigurationTime = 0.0;

        int initialNumberOfGroups = numberOfGroups;

        // variables for saving the home cell information
        int[] homeX = new int[numHosts];
        int[] homeY = new int[numHosts];

        initializeData();

        for (simTime = 0.0; simTime < totalSimulationTime; simTime += stepInterval) {
			if (showRun) {
				component.repaint();
				text.setText(generateStatsString(simTime, contacts));

				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}

            // reconfiguration mechanism
            if (simTime == nextReconfigurationTime) {
                for (int i = 0; i < numberOfRows; i++) {
                    for (int j = 0; j < numberOfColumns; j++) {
                        cells[i][j].numberOfHosts = 0;
                    }
                }

                groups = new int[numHosts][numHosts];

                numberOfReconfigurations = numberOfReconfigurations + 1.0;
                nextReconfigurationTime = reconfigurationInterval * numberOfReconfigurations;

                double previousModth;
                double modth = 0.1;

                if (girvanNewman) {
                    initializeWeightArrayInGroups(interaction, numHosts, initialNumberOfGroups, rewiringProb, threshold);
                    generateAdjacency(interaction, adjacency, threshold, numHosts);

                    // clustering using the Girvan-Newman algorithm
                    do {
                        for (int i = 0; i < numHosts; i++) {
                            numberOfMembers[i] = 0;
                        }

                        double[] betw = new double[numHosts];
                        for (int i = 0; i < numHosts; i++) {
                            betw[i] = 0;
                        }
                        computeBetweenness(betw, adjacency, numHosts);

                        for (int i = 0; i < numHosts; i++) {
                            numberOfMembers[i] = 0;
                        }
                        numberOfGroups = getGroups(adjacency, groups, numberOfMembers, numHosts);

                        if (printOut) {
                            printGroups(numberOfGroups, groups, numberOfMembers, numHosts);
                        }
                        previousModth = modth;

                        modth = splitNetworkByThreshold(adjacency, numHosts, modth);
                    } while ((previousModth < modth) && (modth > -1));
                } else {
                    //communities based on the initial number of caves in the Caveman model (i.e., w = 0)
                    initializeWeightArrayInGroups(interaction, numHosts, initialNumberOfGroups, 0, threshold);
                    generateAdjacency(interaction, adjacency, threshold, numHosts);

                    for (int i = 0; i < numHosts; i++) {
                        numberOfMembers[i] = 0;
                    }

                    numberOfGroups = getGroups(adjacency, groups, numberOfMembers, numHosts);
                    if (printOut) {
                        printGroups(numberOfGroups, groups, numberOfMembers, numHosts);
                    }

                    publishData();

                    if (controlledRewiring) {
                        initializeWeightArrayInGroups(interaction, numHosts, initialNumberOfGroups, threshold);
                    } else {
                        initializeWeightArrayInGroups(interaction, numHosts, initialNumberOfGroups, rewiringProb, threshold);
                    }

                    generateAdjacency(interaction, adjacency, threshold, numHosts);
                }

                int pointer = 0;

                boolean[][] usedCells = new boolean[numberOfRows][numberOfColumns];
                int[][] neighbors = new int[numberOfRows][numberOfColumns];

                for (int i = 0; i < numberOfRows; i++) {
                    for (int j = 0; j < numberOfColumns; j++) {
                        neighbors[i][j] = 0;
                        usedCells[i][j] = false;
                    }
                }

                int allowedNeigh = 1;
                for (int i = 0; i < numberOfGroups; i++) {
                    // avoid that 2 groups are assigned to the same cell
                    //and that they share more than x edges
                    int cellIdX;
                    int cellIdY;
                    boolean neigh;
                    Random rand = new Random(seed);
                    do {
                        neigh = false;
                        cellIdX = rand.nextInt(numberOfRows) + 1;
                        cellIdY = rand.nextInt(numberOfColumns) + 1;

                        if (neighbors[cellIdX - 1][cellIdY - 1] >= allowedNeigh) {
                            neigh = true;
                        }
                    } while (usedCells[cellIdX - 1][cellIdY - 1] || neigh);

                    usedCells[cellIdX - 1][cellIdY - 1] = true;

                    if (cellIdX - 1 > 0) {
                        neighbors[cellIdX - 2][cellIdY - 1]++;
                    }

                    if (cellIdY - 1 > 0) {
                        neighbors[cellIdX - 1][cellIdY - 2]++;
                    }

                    if (cellIdX - 1 < numberOfRows - 1) {
                        neighbors[cellIdX][cellIdY - 1]++;
                    }

                    if (cellIdY - 1 < numberOfColumns - 1) {
                        neighbors[cellIdX - 1][cellIdY]++;
                    }

                    if (cellIdX - 1 > 0 && cellIdY - 1 > 0) {
                        neighbors[cellIdX - 2][cellIdY - 2]++;
                    }

                    if (cellIdX - 1 > 0 && cellIdY - 1 < numberOfColumns - 1) {
                        neighbors[cellIdX - 2][cellIdY]++;
                    }

                    if (cellIdX - 1 < numberOfRows - 1 && cellIdY - 1 > 0) {
                        neighbors[cellIdX][cellIdY - 2]++;
                    }

                    if (cellIdX - 1 < numberOfRows - 1 && cellIdY - 1 < numberOfColumns - 1) {
                        neighbors[cellIdX][cellIdY]++;
                    }

                    homeX[i] = cellIdX;
                    homeY[i] = cellIdY;

                    for (int j = 0; j < numberOfMembers[i]; j++) {
                        int hostId = groups[i][j];
                        hosts[hostId - 1].cellIdX = cellIdX;
                        hosts[hostId - 1].cellIdY = cellIdY;

                        communities[pointer][0] = hostId;
                        communities[pointer][1] = i + 1;
                        pointer++;

                        // increment the number of the hosts in that cell
                        cells[cellIdX - 1][cellIdY - 1].numberOfHosts += 1;

                        // CA: initialise current community
                        currComm[hostId - 1] = (cellIdX - 1) * numberOfColumns + (cellIdY - 1);
                        previousGoalCommunity[hostId - 1] = (cellIdX - 1) * numberOfColumns + (cellIdY - 1);
                        currCommStartTime[hostId - 1] = simTime;
                    }
                }

                // get friend communities for each node
                // compute CA: being t = 0, all nodes are in their home cell
                for (int thisNode = 0; thisNode < numHosts; thisNode++) {
                    for (int c = 0; c < numberOfRows; c++) {
                        for (int r = 0; r < numberOfColumns; r++) {
                            CA[c][r] = 0;
                        }
                    }

                    for (int n = 0; n < numHosts; n++) {
                        if (n != thisNode) {
                            CA[hosts[n].cellIdX - 1][hosts[n].cellIdY - 1] += adjacency[thisNode][n];
                        }
                    }

                    double totAttractivity = 0;
                    for (int c = 0; c < numberOfRows; c++) {
                        for (int r = 0; r < numberOfColumns; r++) {
                            if (cells[c][r].numberOfHosts != 0) {
                                friendCommunity[thisNode][c * numberOfColumns + r] = (CA[c][r] > 0);
                            } else {
                                CA[c][r] = 0;
                                friendCommunity[thisNode][c * numberOfColumns + r] = false;
                            }

                            totAttractivity += CA[c][r];
                        }
                    }

                    // normalisation & output of weights
                    if (printOut) {
                        try {
                            out.write("Node " + String.format("%02d", thisNode));
                            out.newLine();
                            out.write("-------");
                            out.newLine();

                            for (int c = 0; c < numberOfRows; c++) {
                                for (int r = 0; r < numberOfColumns; r++) {
                                    if (c * numberOfColumns + r == (homeX[thisNode % numberOfGroups] - 1) * numberOfColumns + homeY[thisNode % numberOfGroups] - 1) {
                                        int x = (c * numberOfColumns + r);
                                        out.write(String.format("%02d: %,.3f", x, (CA[c][r] / totAttractivity)));
                                        out.newLine();
                                    } else {
                                        int x = c * numberOfColumns + r;
                                        out.write(String.format("%02d: %,.3f", x, (CA[c][r] / totAttractivity)));
                                        out.newLine();
                                    }
                                }
                            }

                            out.newLine();
                        } catch (Exception e) {
                            System.err.println("Error: " + e.getMessage());
                        }
                    }
                }

                if (firstTime && communitiesTraces) {
                    int temp1;
                    int temp2;
                    for (int i = 0; i < numHosts; i++) {
                        for (int j = 0; j < numHosts - 1; j++) {
                            if (communities[j][0] > communities[j + 1][0]) {
                                temp1 = communities[j + 1][0];
                                temp2 = communities[j + 1][1];

                                communities[j + 1][0] = communities[j][0];
                                communities[j + 1][1] = communities[j][1];

                                communities[j][0] = temp1;
                                communities[j][1] = temp2;
                            }
                        }
                    }
                }


                if (firstTime) {
                    if (printOut) {
                        System.out.println("Camp's rule for stationary location distribution DONE");
                    }

                    // insertion of an AP in the middle of the scenario
                    accessPoint.currentX = sideLengthWholeAreaX / 2;
                    accessPoint.currentY = sideLengthWholeAreaY / 2;

                    // definition of the initial position of the hosts
                    for (int k = 0; k < numHosts; k++) {
                        // in the RWP version of CMM the initial positions must follow Camp's rule
                        double r;
                        Random rand = new Random(seed);
                        do {
                            // x1 and y1
                            hosts[k].currentX = cells[hosts[k].cellIdX - 1][hosts[k].cellIdY - 1].minX + rand.nextDouble() * sideLengthX;
                            hosts[k].currentY = cells[hosts[k].cellIdX - 1][hosts[k].cellIdY - 1].minY + rand.nextDouble() * sideLengthY;

                            // x2 and y2
                            hosts[k].goalCurrentX = cells[hosts[k].cellIdX - 1][hosts[k].cellIdY - 1].minX + rand.nextDouble() * sideLengthX;
                            hosts[k].goalCurrentY = cells[hosts[k].cellIdX - 1][hosts[k].cellIdY - 1].minY + rand.nextDouble() * sideLengthY;

                            r = Math.pow(Math.pow(hosts[k].goalCurrentX - hosts[k].currentX, 2)
                                    + Math.pow(hosts[k].goalCurrentY - hosts[k].currentY, 2), 1 / 2) / Math.sqrt(2);

                        } while (rand.nextDouble() >= r);

                        double unif2 = rand.nextDouble();
                        hosts[k].currentX = unif2 * hosts[k].currentX + (1 - unif2) * hosts[k].goalCurrentX;
                        hosts[k].currentY = unif2 * hosts[k].currentY + (1 - unif2) * hosts[k].goalCurrentY;

                        hosts[k].previousGoalX = hosts[k].currentX;
                        hosts[k].previousGoalY = hosts[k].currentY;
                    }

                    if (printOut) {
                        System.out.println(" ====== DONE FOR ALL =======");
                    }

                    firstTime = false;

                    // trace generation
                    if (nsTrace) {
                        // generating initial positions of the hosts
                        for (int i = 0; i < numHosts; i++) {
                            System.out.println("$node_" + i + ") set X_" + hosts[i].currentX);
                            System.out.println("$node_" + i + ") set Y_" + hosts[i].currentY);
                            System.out.println("$node_" + i + ") set Z_" + 0.0);
                        }

                        System.out.println("$node_" + numHosts + ") set X_" + accessPoint.currentX);
                        System.out.println("$node_" + numHosts + ") set Y_" + accessPoint.currentY);
                        System.out.println("$node_" + numHosts + ") set Z_" + 0.0);

                        System.out.println(simTime + "$node_(" + numHosts + ") setdest" + accessPoint.currentX
                                + " " + accessPoint.currentY + " " + 0.0);
                    }
                }

                // definition of the initial goals
                Random rand = new Random(seed);
                if (!firstTime) {
                    // when firstTime == true this process follow the Camp's rule (see above)
                    for (int k = 0; k < numHosts; k++) {
                        hosts[k].goalCurrentX = cells[hosts[k].cellIdX - 1][hosts[k].cellIdY - 1].minX + rand.nextDouble() * sideLengthX;
                        hosts[k].goalCurrentY = cells[hosts[k].cellIdX - 1][hosts[k].cellIdY - 1].minY + rand.nextDouble() * sideLengthY;
                    }
                }

                // generation of the traces - setting of the goals
                for (int i = 0; i < numHosts; i++) {
                    hosts[i].absSpeed = hosts[i].speed;

                    if (nsTrace) {
                        System.out.println(simTime + " $node_(" + i + ") setdest "
                                + hosts[i].goalCurrentX + " " + hosts[i].goalCurrentY
                                + " " + (hosts[i].absSpeed) / stepInterval);
                    }
                }
            }

            for (int i = 0; i < numHosts; i++) {
                // CA: detect a node's community (check node's position)
                newComm = (int) Math.floor(hosts[i].currentX / sideLengthX) * numberOfColumns + (int) Math.floor(hosts[i].currentY / sideLengthY);

                if (newComm != currComm[i]) {
                    frequencies[i][currComm[i]] += simTime - currCommStartTime[i];
                    currComm[i] = newComm;
                    currCommStartTime[i] = simTime;
                }

                if ((hosts[i].currentX > hosts[i].goalCurrentX + hosts[i].speed)
                        || (hosts[i].currentX < hosts[i].goalCurrentX - hosts[i].speed)
                        || (hosts[i].currentY > hosts[i].goalCurrentY + hosts[i].speed)
                        || (hosts[i].currentY < hosts[i].goalCurrentY - hosts[i].speed)) {
                    // move towards the goal
                    if (hosts[i].currentX < (hosts[i].goalCurrentX - hosts[i].speed)) {
                        hosts[i].currentX = hosts[i].currentX + hosts[i].speed;
                    }
                    if (hosts[i].currentX > (hosts[i].goalCurrentX + hosts[i].speed)) {
                        hosts[i].currentX = (hosts[i].currentX) - hosts[i].speed;
                    }
                    if (hosts[i].currentY < (hosts[i].goalCurrentY - hosts[i].speed)) {
                        hosts[i].currentY = (hosts[i].currentY) + hosts[i].speed;
                    }
                    if (hosts[i].currentY > (hosts[i].goalCurrentY + hosts[i].speed)) {
                        hosts[i].currentY = (hosts[i].currentY) - hosts[i].speed;
                    }
                } else {
                    int selectedGoalCellX = 0;
                    int selectedGoalCellY = 0;
                    int previousGoalCellX = hosts[i].cellIdX;
                    int previousGoalCellY = hosts[i].cellIdY;

                    if (deterministic) {
                        // algorithm for the selection of the new cell
                        for (int c = 0; c < numberOfRows; c++) {
                            for (int r = 0; r < numberOfColumns; r++) {
                                CA[c][r] = 0;
                            }
                        }

                        for (int n = 0; n < numHosts; n++) {
                            if (n != i) {
                                CA[hosts[n].cellIdX - 1][hosts[n].cellIdY - 1] += interaction[i][n];
                            }
                        }

                        for (int c = 0; c < numberOfRows; c++) {
                            for (int r = 0; r < numberOfColumns; r++) {
                                if (cells[c][r].numberOfHosts != 0) {
                                    CA[c][r] = (float) ((double) CA[c][r] / (double) cells[c][r].numberOfHosts);
                                } else {
                                    CA[c][r] = 0;
                                }
                            }
                        }

                        int selectedGoalCellX2 = 0;
                        int selectedGoalCellY2 = 0;

                        double CAMax1 = 0;
                        double CAMax2 = 0;

                        for (int c = 0; c < numberOfRows; c++) {
                            for (int r = 0; r < numberOfColumns; r++) {
                                if (CA[c][r] > CAMax1) {
                                    // set the second best
                                    selectedGoalCellX2 = selectedGoalCellX;
                                    selectedGoalCellY2 = selectedGoalCellY;
                                    CAMax2 = CAMax1;

                                    selectedGoalCellX = c + 1;
                                    selectedGoalCellY = r + 1;

                                    CAMax1 = CA[c][r];

                                } else if (CA[c][r] > CAMax2) {
                                    selectedGoalCellX2 = c + 1;
                                    selectedGoalCellY2 = r + 1;
                                    CAMax2 = CA[c][r];
                                }
                            }
                        }

                        Random rand = new Random(seed);

                        // HIBOP - the traveler stays within a cell with probability p
                        if (hosts[i].isATraveler) {
                            if (rand.nextDouble() < travProb) {
                                selectedGoalCellX = previousGoalCellX;
                                selectedGoalCellY = previousGoalCellY;
                            } else {
                                if ((previousGoalCellX == selectedGoalCellX) && (previousGoalCellY == selectedGoalCellY)) {
                                    if (selectedGoalCellX != 0) {
                                        selectedGoalCellX = selectedGoalCellX2;
                                        selectedGoalCellY = selectedGoalCellY2;
                                    }
                                }
                            }
                        }
                    } else {
                        Random rand = new Random(seed);

                        // compute cell attractivity
                        if (!isHome(previousGoalCellX, previousGoalCellY, homeX[i % numberOfGroups], homeY[i % numberOfGroups])) {
                            // if I am not in my home...
                            if (rand.nextDouble() < remainingProb) {
                                // ... remain outside with probability remainingProb
                                selectedGoalCellX = previousGoalCellX;
                                selectedGoalCellY = previousGoalCellY;
                            } else {
                                // ... go back home with probability remainingProb
                                selectedGoalCellX = homeX[i % numberOfGroups];
                                selectedGoalCellY = homeY[i % numberOfGroups];
                            }
                        } else {
                            // algorithm for the selection of the new cell
                            for (int c = 0; c < numberOfRows; c++) {
                                for (int r = 0; r < numberOfColumns; r++) {
                                    CA[c][r] = 0.0f;
                                }
                            }

                            // WRECOM 07
                            for (int n = 0; n < numHosts; n++) {
                                int groupOfI = i % numberOfGroups;
                                int groupOfN = n % numberOfGroups;

                                if (n != i) {
                                    // if n and i belong to different groups...
                                    if (groupOfN != groupOfI) {
                                        // ... take into account the real "weight" of the external relationship
                                        // (each node is accounted for when computing the attractivity of its home cell)
                                        CA[homeX[groupOfN] - 1][homeY[groupOfN] - 1] += adjacency[i][n];
                                    } else {
                                        // if n and i belong to the same group, count n in the home's attractivity
                                        CA[homeX[groupOfI] - 1][homeY[groupOfI] - 1] += adjacency[i][n];
                                    }
                                }
                            }

                            for (int c = 0; c < numberOfRows; c++) {
                                for (int r = 0; r < numberOfColumns; r++) {
                                    if (cells[c][r].numberOfHosts == 0) {
                                        CA[c][r] = 0;
                                    }
                                }
                            }

                            // denominator for the normalization of the values
                            float denNorm = 0.00f;

                            // HIBOP
                            drift = 0.0f;

                            for (int c = 0; c < numberOfRows; c++) {
                                for (int r = 0; r < numberOfColumns; r++) {
                                    denNorm = denNorm + CA[c][r] + drift;
                                }
                            }

                            for (int c = 0; c < numberOfRows; c++) {
                                for (int r = 0; r < numberOfColumns; r++) {
                                    if (CA[c][r] == 0) {
                                        a[c][r] = drift / denNorm;
                                    } else {
                                        a[c][r] = (CA[c][r] + drift) / denNorm;
                                    }
                                }
                            }

                            float current = 0.0f;
                            for (int c = 0; c < numberOfRows; c++) {
                                for (int r = 0; r < numberOfColumns; r++) {
                                    p[c][r].min = current;
                                    p[c][r].max = p[c][r].min + a[c][r];
                                    current = p[c][r].max;
                                }
                            }

                            float infiniteDice = (float) rand.nextDouble();

                            for (int c = 0; c < numberOfRows; c++) {
                                for (int r = 0; r < numberOfColumns; r++) {
                                    if ((infiniteDice > p[c][r].min) && (infiniteDice < p[c][r].max)) {

                                        selectedGoalCellX = c + 1;
                                        selectedGoalCellY = r + 1;
                                    }
                                }
                            }

                            if (!isHome(previousGoalCellX, previousGoalCellY, homeX[i % numberOfGroups], homeY[i % numberOfGroups])) {
                                // node wasn't in its home cell
                                if (previousGoalCellX != selectedGoalCellX || previousGoalCellY != selectedGoalCellY) {
                                    // node has selected a goal in a different cell
                                    if (rand.nextDouble() < remainingProb) {
                                        selectedGoalCellX = previousGoalCellX;
                                        selectedGoalCellY = previousGoalCellY;
                                    }
                                }
                            }
                        }
                    }

					if (selectedGoalCellX == 0) {
						selectedGoalCellX = previousGoalCellX;
					}

					if (selectedGoalCellY == 0) {
						selectedGoalCellY = previousGoalCellY;
					}

                    // re-definition of the number of hosts in each cell
                    cells[previousGoalCellX - 1][previousGoalCellY - 1].numberOfHosts--;
                    cells[selectedGoalCellX - 1][selectedGoalCellY - 1].numberOfHosts++;

                    previousGoalCommunity[i] = (previousGoalCellX - 1) * numberOfColumns + (previousGoalCellY - 1);

                    Random rand = new Random(seed);
                    double newGoalRelativeX = rand.nextDouble() * sideLengthX;
                    double newGoalRelativeY = rand.nextDouble() * sideLengthY;

                    // refresh of the information
                    hosts[i].cellIdX = selectedGoalCellX;
                    hosts[i].cellIdY = selectedGoalCellY;

                    double distance = Math.sqrt(Math.pow(hosts[i].goalCurrentX - hosts[i].previousGoalX, 2)
                            + Math.pow(hosts[i].goalCurrentY - hosts[i].previousGoalY, 2));
                    distances[i] += distance;

                    hosts[i].previousGoalX = hosts[i].goalCurrentX;
                    hosts[i].previousGoalY = hosts[i].goalCurrentY;

                    hosts[i].goalCurrentX = cells[selectedGoalCellX - 1][selectedGoalCellY - 1].minX + newGoalRelativeX;
                    hosts[i].goalCurrentY = cells[selectedGoalCellX - 1][selectedGoalCellY - 1].minY + newGoalRelativeY;
                    hosts[i].absSpeed = hosts[i].speed;

                    if (nsTrace) {
                        System.out.println("$ns_at " + simTime + " $node_(" + i + ") setdest " + hosts[i].goalCurrentX
                                + " " + hosts[i].goalCurrentY + " " + ((hosts[i].absSpeed) / stepInterval));
                    }
                }
            }

            for (int i = 0; i < numHosts; i++) {
                // update connectivity
                for (int j = 0; j < numHosts; j++) {
                    if (i != j) {
                        // calculation of the current distance
                        double currentDistance = Math.sqrt((hosts[i].currentX - hosts[j].currentX)
                                * (hosts[i].currentX - hosts[j].currentX)
                                + (hosts[i].currentY - hosts[j].currentY)
                                * (hosts[i].currentY - hosts[j].currentY));

                        // if currentDistance <= radius then the hosts are connected
                        if (currentDistance < radius) {
                            // if the hosts has been previously disconnected, then they must be connected
                            if (!isConnected[i][j]) {
                                isConnected[i][j] = true;
                                lastValues[i][j] = simTime;
                                startContact(i, j, simTime);
                                contacts++;
                            }
                        } else {
                            if (isConnected[i][j]) {
                                if (simTime != 0) {
                                    // if the hosts has been previously connected, then they must be disconnected
                                    isConnected[i][j] = false;
                                    endContact(i, j, simTime);
                                }
                            }
                        }
                    }
                }

                // do the same things for the AP
                double currentDistance = Math.sqrt((hosts[i].currentX - accessPoint.currentX)
                        * (hosts[i].currentX - accessPoint.currentX)
                        + (hosts[i].currentY - accessPoint.currentY)
                        * (hosts[i].currentY - accessPoint.currentY));
                if (currentDistance < radius) {
                    // if the host and the AP have been previously disconnected, then they must be connected
                    if (!isConnectedWithAP[i]) {
                        isConnectedWithAP[i] = true;
                        lastValuesWithAP[i] = simTime;
                    }
                } else {
                    // else they are disconnected
                    if (isConnectedWithAP[i]) {
                        if (simTime != 0) {
                            // if the hosts have been previously connected, then they must be disconnected
                            isConnectedWithAP[i] = false;
                        }
                    }
                }
            }
        }

        // finish the simulation
        for (int i = 0; i < numHosts; i++) {
            // CA
            frequencies[i][currComm[i]] += simTime - currCommStartTime[i];

            for (int j = 0; j < numHosts; j++) {
                if (isConnected[i][j] && simTime != 0) {
                    //if the hosts have been previously connected, then they must be disconnected
                    isConnected[i][j] = false;
                }
            }

            if (isConnectedWithAP[i] == true) {
                // if the hosts have been previously connected, then they must be disconnected
                isConnectedWithAP[i] = false;
            }
        }

		if (showRun) {
			frame.setVisible(false);
			frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
		}

        if (printOut) {
            try {
                out.write("Distances Travelled");
                out.newLine();
                out.write("===================");
                out.newLine();

                for (int i = 0; i < numHosts; i++) {
                    out.write(String.format("Node %02d: %f", i, distances[i]));
                    out.newLine();
                }

                out.close();
            } catch (IOException ex) {
                Logger.getLogger(HCMM.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Initializes tracing data.
     */
    protected abstract void initializeData();

    /**
     * Publishes tracing data.
     */
    protected abstract void publishData();

    /**
     * Signals the start of a contact.
     *
     * @param nodeA first node of the contact
     * @param nodeB second node of the contact
     * @param tick timestamp
     */
    protected abstract void startContact(int nodeA, int nodeB, double tick);

    /**
     * Signals the end of a contact.
     *
     * @param nodeA first node of the contact
     * @param nodeB second node of the contact
     * @param tick timestamp
     */
    protected abstract void endContact(int nodeA, int nodeB, double tick);

    /**
     * Class representing an HCMM host.
     */
    public static class Host {

        public double currentX;
        public double currentY;
        double relativeX;
        double relativeY;
        double goalRelativeX;
        double goalRelativeY;
        double goalCurrentX;
        double goalCurrentY;
        double previousGoalX;
        double previousGoalY;
        int cellIdX;
        int cellIdY;
        double speed;
        double absSpeed;
        double af;
        boolean isATraveler;
    }

    /**
     * Class representing an HCMM cell.
     */
    private class CellsItem {

        double minX;
        double minY;
        int numberOfHosts;
    }

    /**
     * Class representing a probability range.
     */
    private class ProbRange {

        float min;
        float max;
    }
}
