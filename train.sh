#!/bin/bash
#java -Xmx15000m -cp out/medlda.jar cn.clickwise.classify.sspm.svm_struct_main -c 5000 metrics_NN_NR_L5/ntrainSample_NN_NR_L5.txt metrics_NN_NR_L5/model_NN_NR_L5

#java -Xmx10000m -cp out/medlda.jar cn.clickwise.classify.sspm.svm_struct_classify metrics_NN_NR_L5/ntestSample_NN_NR_L5.txt metrics_NN_NR_L5/model_NN_NR_L5 metrics_NN_NR_L5/prediction

#java -Xmx15000m -cp out/medlda.jar cn.clickwise.classify.sspm.svm_struct_main -c 5000 example/train.txt example/model

#java -Xmx10000m -cp out/medlda.jar cn.clickwise.classify.sspm.svm_struct_classify example/test.txt example/model example/prediction


java -Xmx10000m -cp out/medlda.jar cn.clickwise.medlda.Main est 120 29 10 16 64 temp random


#java -cp out/medlda.jar cn.clickwise.medlda.Main inf 29 temp120_c16_f10
