#!/bin/bash
#java -Xmx30000m -cp out/medlda.jar cn.clickwise.classify.sspm.svm_struct_main -c 5000 tb_1224_ngram/nltrain_1224_ngram.txt tb_1224_ngram/model_1224_lngram

java -Xmx10000m -cp out/medlda.jar cn.clickwise.classify.sspm.svm_struct_classify tb_1224_ngram/nltest_1224_ngram.txt tb_1224_ngram/model_1224_lngram tb_1224_ngram/prediction
#java -Xmx15000m -cp out/medlda.jar cn.clickwise.classify.sspm.svm_struct_main -c 5000 metrics_NN_NR_L5/ntrainSample_NN_NR_L5.txt metrics_NN_NR_L5/model_NN_NR_L5

#java -Xmx10000m -cp out/medlda.jar cn.clickwise.classify.sspm.svm_struct_classify metrics_NN_NR_L5/ntestSample_NN_NR_L5.txt metrics_NN_NR_L5/model_NN_NR_L5 metrics_NN_NR_L5/prediction

#java -Xmx15000m -cp out/medlda.jar cn.clickwise.classify.sspm.svm_struct_main -c 5000 example/train.txt example/model


#java -Xmx10000m -cp out/medlda.jar cn.clickwise.medlda.Main est 120 566 10 16 64 temp random
#java -Xmx10000m -cp out/medlda.jar cn.clickwise.classify.sspm.svm_struct_classify example/test.txt example/model example/prediction


java -Xmx1500m -cp out/medlda.jar cn.clickwise.medlda.Main est 80 29 10 16 64 temp random


#java -Xmx5000m -cp out/medlda.jar cn.clickwise.medlda.Main inf 29 temp120_c16_f10 0.02 0.03 0.08 0.1 0.2 0.5 0.8 0.9 0.95 0.98
#java -Xmx5000m -cp out/medlda.jar cn.clickwise.medlda.Main inf 29 temp120_c16_f10
