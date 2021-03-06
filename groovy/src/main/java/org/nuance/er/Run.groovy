package org.nuance.er

import org.linqs.psl.application.inference.MPEInference
import org.linqs.psl.application.learning.weight.TrainingMap
import org.linqs.psl.application.learning.weight.VotedPerceptron
import org.linqs.psl.application.learning.weight.maxlikelihood.MaxLikelihoodMPE
import org.linqs.psl.config.Config
import org.linqs.psl.database.DataStore
import org.linqs.psl.database.Database
import org.linqs.psl.database.Partition
import org.linqs.psl.database.atom.PersistedAtomManager
import org.linqs.psl.database.loading.Inserter
import org.linqs.psl.database.rdbms.RDBMSDataStore
import org.linqs.psl.database.rdbms.driver.H2DatabaseDriver
import org.linqs.psl.database.rdbms.driver.H2DatabaseDriver.Type
import org.linqs.psl.evaluation.statistics.RankingEvaluator
import org.linqs.psl.groovy.PSLModel
import org.linqs.psl.model.atom.GroundAtom
import org.linqs.psl.model.predicate.StandardPredicate
import org.linqs.psl.model.term.Constant
import org.linqs.psl.model.term.ConstantType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Paths

class Run {
    private static final String PARTITION_LEARN_OBSERVATIONS = "learn_observations"
    private static final String PARTITION_LEARN_TARGETS = "learn_targets"
    private static final String PARTITION_LEARN_TRUTH = "learn_truth"

    private static final String PARTITION_EVAL_OBSERVATIONS = "eval_observations"
    private static final String PARTITION_EVAL_TARGETS = "eval_targets"
    private static final String PARTITION_EVAL_TRUTH = "eval_truth"

    private static final String DATA_PATH = Paths.get("..", "psl").toString()
    private static final String OUTPUT_PATH = "inferred-predicates"

    private static Logger log = LoggerFactory.getLogger(Run.class)

    private DataStore dataStore
    private PSLModel model

    Run() {

        String suffix = System.getProperty("user.name") + "@" + getHostname()
        String baseDBPath = Config.getString("dbpath", System.getProperty("java.io.tmpdir"))
        String dbPath = Paths.get(baseDBPath, this.getClass().getName() + "_" + suffix).toString()
        dataStore = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, dbPath, true))
        // dataStore = new RDBMSDataStore(new PostgreSQLDriver("psl", true), config)

        model = new PSLModel(this, dataStore)
    }

    /**
     * Defines the logical predicates used in this model
     */
    private void definePredicates() {
        model.add predicate: "SimTitle", types: [ConstantType.UniqueStringID, ConstantType.UniqueStringID]
        model.add predicate: "SimVenue", types: [ConstantType.UniqueStringID, ConstantType.UniqueStringID]
        model.add predicate: "SimYear", types: [ConstantType.UniqueStringID, ConstantType.UniqueStringID]
        model.add predicate: "SimAuthor", types: [ConstantType.UniqueStringID, ConstantType.UniqueStringID]
        model.add predicate: "SameAs", types: [ConstantType.UniqueStringID, ConstantType.UniqueStringID]
//        model.add predicate: "Sub", types: [ConstantType.UniqueIntID, ConstantType.UniqueIntID]
    }

    /**
     * Defines the rules for this model.
     */
    private void defineRules() {
        log.info("Defining model rules")

        model.addRules("""
			30: SimTitle(P1, P2)  -> SameAs(P1, P2) ^2
			5: !SimAuthor(P1, P2) -> !SameAs(P1, P2) ^2
			5: !SimVenue(P1, P2)  -> !SameAs(P1, P2) ^2
			60: !SimYear(P1, P2)  -> !SameAs(P1, P2) ^2
			1: !SameAs(P1, P2)
		""")
//        0.1: !SimName(P1, P2)  -> !SameAs(P1, P2) ^2
//			25: SimName(P1, P2) & SimDescription(P1, P2) & HavePrice(P1) & HavePrice(P2) & SimPrice(P1, P2) -> SameAs(P1, P2) ^2

//        0010: HavePrice(P1) & HavePrice(P2) & !SimPrice(P1, P2) -> !SameAs(P1, P2) ^2

        log.debug("model: {}", model)
    }

    /**
     * Load data from text files into the DataStore.
     * Three partitions are defined and populated: observations, targets, and truth.
     * Observations contains evidence that we treat as background knowledge and use to condition our inferences.
     * Targets contains the inference targets - the unknown variables we wish to infer.
     * Truth contains the true values of the inference variables and will be used to evaluate the model's performance.
     */
    private void loadData() {
        log.info("Loading data into database")

        for (String type : ["learn", "eval"]) {
            Partition obsPartition = dataStore.getPartition(type + "_observations")
            Partition targetsPartition = dataStore.getPartition(type + "_targets")
            Partition truthPartition = dataStore.getPartition(type + "_truth")

            for (StandardPredicate predicate : dataStore.getRegisteredPredicates()) {
                String path = Paths.get(DATA_PATH, type, predicate.getName() + "_obs.txt").toString()
                Inserter inserter = dataStore.getInserter(predicate, obsPartition)
                println(path)
                inserter.loadDelimitedDataTruth(path)
            }

            Inserter inserter = dataStore.getInserter(SameAs, targetsPartition)
            inserter.loadDelimitedData(Paths.get(DATA_PATH, type, "SAMEAS_targets.txt").toString())

            inserter = dataStore.getInserter(SameAs, truthPartition)
            inserter.loadDelimitedDataTruth(Paths.get(DATA_PATH, type, "SAMEAS_truth.txt").toString())
        }
    }

    /**
     * Use the training data to learn weights for our rules and store them back in the model object.
     */
    private void learnWeights() {
        log.info("Starting weight learning")

        Partition obsPartition = dataStore.getPartition(PARTITION_LEARN_OBSERVATIONS)
        Partition targetsPartition = dataStore.getPartition(PARTITION_LEARN_TARGETS)
        Partition truthPartition = dataStore.getPartition(PARTITION_LEARN_TRUTH)

        Set<StandardPredicate> closedPredicates = [
                SimName, SimDescription, SimPrice
        ] as Set

        // This database contains all the ground atoms (targets) that we want to infer.
        // It also includes the observed data (because we will run inference over this db).
        Database randomVariableDatabase = dataStore.getDatabase(targetsPartition, closedPredicates, obsPartition)

        // This database only contains the true ground atoms.
        Database observedTruthDatabase = dataStore.getDatabase(truthPartition, dataStore.getRegisteredPredicates())

        MyVotedPerceptron vp = new MyMaxLikelihoodMPE(model, randomVariableDatabase, observedTruthDatabase)
        vp.learn()

//        for (Rule rule : model.getRules()) {
//            try {
//                WeightedRule weightedRule = (WeightedRule) rule
//                log.info("Rule {} has weight {}", weightedRule.toString(), weightedRule.getWeight())
//            }
//            catch (Exception e) {
//                e.printStackTrace()
//            }
//        }
        randomVariableDatabase.close()
        observedTruthDatabase.close()
        log.info("Weight learning complete")
    }

    /**
     * Run inference to infer the unknown HasCat relationships between people.
     */
    private void runInference() {
        log.info("Starting inference")

        Partition obsPartition = dataStore.getPartition(PARTITION_EVAL_OBSERVATIONS)
        Partition targetsPartition = dataStore.getPartition(PARTITION_EVAL_TARGETS)

        Set<StandardPredicate> closedPredicates = [
                SimName, SimDescription, SimPrice
        ] as Set

        Database inferDB = dataStore.getDatabase(targetsPartition, closedPredicates, obsPartition)

        MPEInference mpe = new MPEInference(model, inferDB)
        mpe.inference()

        mpe.close()
        inferDB.close()

        log.info("Inference complete")
    }

    /**
     * Writes the output of the model into a file
     */
    private void writeOutput() {
        (new File(OUTPUT_PATH)).mkdirs()

        Database resultsDB = dataStore.getDatabase(dataStore.getPartition(PARTITION_EVAL_TARGETS))

        for (StandardPredicate predicate : [SameAs]) {
            FileWriter writer = new FileWriter(Paths.get(OUTPUT_PATH, predicate.getName() + ".txt").toString())

            for (GroundAtom atom : resultsDB.getAllGroundAtoms(predicate)) {
                for (Constant argument : atom.getArguments()) {
                    writer.write(argument.toString() + "\t")
                }
                writer.write("" + atom.getValue() + "\n")
            }

            writer.close()
        }

        resultsDB.close()
    }

    /**
     * Run statistical evaluation scripts to determine the quality of the inferences
     * relative to the defined truth.
     * Note that the target predicate is categorical and we will assign the category with the
     * highest truth value as true and the rest false.
     */
    private void evalResults() {
        Set<StandardPredicate> closedPredicates = [
                SimName, SimDescription, SimPrice

        ] as Set

        // Because the truth data also includes observed data, we will make sure to include the observed
        // partition here.
        Database resultsDB = dataStore.getDatabase(dataStore.getPartition(PARTITION_EVAL_TARGETS),
                closedPredicates, dataStore.getPartition(PARTITION_EVAL_OBSERVATIONS))
        Database truthDB = dataStore.getDatabase(dataStore.getPartition(PARTITION_EVAL_TRUTH),
                dataStore.getRegisteredPredicates())

        for (StandardPredicate predicate : [SameAs]) {
            for (double threshold : [0.5, 0.9]) {
                RankingEvaluator evaluator = new RankingEvaluator(0.5)

                PersistedAtomManager atomManager = new PersistedAtomManager(resultsDB)
                TrainingMap trainingMap = new TrainingMap(atomManager, truthDB, true)

                evaluator.compute(trainingMap, predicate)
//                RankingScore stats = comparator.compare(predicate)

                log.info("Threshold:  {}", threshold)
                log.info(evaluator.getAllStats())
            }
        }

        resultsDB.close()
        truthDB.close()
    }

    void run() {
        definePredicates()
        defineRules()
        loadData()

        learnWeights()
        runInference()

        writeOutput()
        evalResults()

        dataStore.close()
    }

    /**
     * Run this model from the command line
     * @param args - the command line arguments
     */
    static void main(String[] args) {
        Run run = new Run()
        run.run()
    }

    private static String getHostname() {
        String hostname = "unknown"

        try {
            hostname = InetAddress.getLocalHost().getHostName()
        } catch (UnknownHostException ignored) {
            log.warn("Hostname can not be resolved, using '" + hostname + "'.")
        }

        return hostname
    }
}
