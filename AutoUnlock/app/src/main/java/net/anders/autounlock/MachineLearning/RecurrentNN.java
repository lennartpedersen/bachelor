package net.anders.autounlock.MachineLearning;

/**
 * Created by tom-fire on 16/05/2017.
 */

import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class RecurrentNN {

    public static void main(String[] args) {
        createAndUseNetwork();
    }

    private static void createAndUseNetwork() {
        DenseLayer inputLayer = new DenseLayer.Builder()
                .nIn(2)
                .nOut(3)
                .name("Input")
                .build();

        DenseLayer hiddenLayer = new DenseLayer.Builder()
                .nIn(3)
                .nOut(2)
                .name("Hidden")
                .build();

        RnnOutputLayer outputLayer = new RnnOutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                .activation(Activation.SOFTMAX)
                .nIn(2)
                .nOut(1)
                .name("Output")
                .build();

        NeuralNetConfiguration.Builder nncBuilder = new NeuralNetConfiguration.Builder();
        nncBuilder.iterations(10000);
        nncBuilder.learningRate(0.01);


        NeuralNetConfiguration.ListBuilder listBuilder = nncBuilder.list();
        listBuilder.layer(0, inputLayer);
        listBuilder.layer(1, hiddenLayer);
        listBuilder.layer(2, outputLayer);

        //listBuilder.backprop(true);
        listBuilder.backpropType(BackpropType.TruncatedBPTT)
                .tBPTTForwardLength(100)
                .tBPTTBackwardLength(100);

        MultiLayerNetwork myNetwork = new MultiLayerNetwork(listBuilder.build());
        myNetwork.init();


        final int NUM_SAMPLES = 4;

        INDArray trainingInputs = Nd4j.zeros(NUM_SAMPLES, inputLayer.getNIn());
        INDArray trainingOutputs = Nd4j.zeros(NUM_SAMPLES, outputLayer.getNOut());

        // If 0,0 show 0
        trainingInputs.putScalar(new int[]{0,0}, 0);
        trainingInputs.putScalar(new int[]{0,1}, 0);
        trainingOutputs.putScalar(new int[]{0,0}, 0);

        // If 0,1 show 1
        trainingInputs.putScalar(new int[]{1,0}, 0);
        trainingInputs.putScalar(new int[]{1,1}, 1);
        trainingOutputs.putScalar(new int[]{1,0}, 1);

        // If 1,0 show 1
        trainingInputs.putScalar(new int[]{2,0}, 1);
        trainingInputs.putScalar(new int[]{2,1}, 0);
        trainingOutputs.putScalar(new int[]{2,0}, 1);

        // If 1,1 show 0
        trainingInputs.putScalar(new int[]{3,0}, 1);
        trainingInputs.putScalar(new int[]{3,1}, 1);
        trainingOutputs.putScalar(new int[]{3,0}, 0);

        DataSet myData = new DataSet(trainingInputs, trainingOutputs);

        myNetwork.fit(myData);

        System.out.println(myNetwork.output(trainingInputs));
    }
}
