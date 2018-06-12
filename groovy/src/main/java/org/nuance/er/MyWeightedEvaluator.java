package org.nuance.er;

import org.linqs.psl.application.learning.weight.TrainingMap;
import org.linqs.psl.evaluation.statistics.ContinuousEvaluator;
import org.linqs.psl.model.atom.GroundAtom;
import org.linqs.psl.model.predicate.StandardPredicate;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MyWeightedEvaluator extends ContinuousEvaluator {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(MyWeightedEvaluator.class);

    private int newCount;
    private double weightedError;
    private double weight;


    public MyWeightedEvaluator() {
        super(RepresentativeMetric.MSE);
        newCount = 0;
    }

    public void setWeight(double weight){
        this.weight = weight;
    }

    public void setWeightByTrainingMap(TrainingMap trainingMap, StandardPredicate predicate) {
        int positiveCount = 0;
        int negativeCount = 0;

        for (Map.Entry<GroundAtom, GroundAtom> entry : trainingMap.getFullMap()) {
            if (predicate != null && entry.getKey().getPredicate() != predicate) {
                continue;
            }

            if (entry.getValue().getValue() == 1.0) {
                positiveCount++;
            } else {
                negativeCount++;
            }
        }

        log.debug("Count: " + positiveCount + " " + negativeCount);
        weight = negativeCount * 1.0 / positiveCount;
    }


    public double getRepresentativeMetric(){
        return weightedError;
    }

    @Override
    public void compute(TrainingMap trainingMap, StandardPredicate predicate) {
        weightedError = 0;
        for (Map.Entry<GroundAtom, GroundAtom> entry : trainingMap.getFullMap()) {
            if (predicate != null && entry.getKey().getPredicate() != predicate) {
                continue;
            }

            double currentWeight;
            if (entry.getValue().getValue() == 1.0) {
                currentWeight = weight;
            } else {
                currentWeight = 1;
            }
            weightedError += currentWeight * Math.pow(entry.getValue().getValue() - entry.getKey().getValue(), 2);
        }
    }
}
